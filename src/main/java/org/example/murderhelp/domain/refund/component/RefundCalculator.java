package org.example.murderhelp.domain.refund.component;

import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.refund.dto.RefundRequest;
import org.example.murderhelp.domain.refund.dto.RefundRequest.RefundItemRequest;
import org.example.murderhelp.domain.refund.entity.RefundItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RefundCalculator {

    public record RefundCalculationResult(
            List<RefundItem> refundItems,
            long totalPgRefundAmount
    ) {}

    public RefundCalculationResult calculate(
            RefundRequest request,
            Map<Long, OrderItem> orderItemMap,
            Payment payment,
            boolean isFullRefund,
            long refundedPgAmount
    ) {
        long totalPgRefundAmount = 0;

        if (isFullRefund) {
            // [분기 A: 전액/마지막 환불]
            // 비율로 쪼개면 1원 단위 오차가 생길 수 있으므로, '원래 결제 총액'에서 '기존에 환불받은 총액'을 통째로 뺀다.
            totalPgRefundAmount = payment.getPgAmount() - refundedPgAmount;
           log.info("전액/마지막 환불 금액 배정: PG={}", totalPgRefundAmount);
        }

        // 요청 전체를 RefundItem 리스트로 변환
        List<RefundItem> generatedRefundItems = buildRefundItems(request, orderItemMap, payment, isFullRefund, totalPgRefundAmount);

        // 부분 환불일 경우, 개별 계산된 금액의 합산을 총액으로 갱신
        if (!isFullRefund) {
           totalPgRefundAmount = generatedRefundItems.stream().mapToLong(RefundItem::getPgRefundAmount).sum();
            log.info("부분 환불 개별 항목 합산 금액: PG={}", totalPgRefundAmount);
        }

        return new RefundCalculationResult(generatedRefundItems, totalPgRefundAmount);
    }

    /**
     * 요청 전체를 RefundItem 리스트로 변환 (전액 환불 시 마지막 잔액 몰아주기 처리)
     */
    private List<RefundItem> buildRefundItems(
            RefundRequest request,
            Map<Long, OrderItem> orderItemMap,
            Payment payment,
            boolean isFullRefund,
            Long totalPgRefundAmount) {
        
        List<RefundItem> items = new ArrayList<>();
        List<RefundItemRequest> requestItems = request.items();

        if (isFullRefund) {
            // [분기 A 전액/마지막 환불] 마지막 항목에 남은 잔액을 전부 통째로 맞춤
            List<RefundItemRequest> subList = requestItems.subList(0, requestItems.size() - 1);
            long accumulatedPgItemRefund = 0;

            for (RefundItemRequest requestItem : subList) {
                RefundItem refundItem = allocateByRatio(requestItem, orderItemMap, payment);
                accumulatedPgItemRefund += refundItem.getPgRefundAmount();
                items.add(refundItem);
            }

            // 마지막 아이템은 비율로 계산하지 않고 (구해둔 총 환불액 - 여태까지 담은 환불액)을 넣음
            RefundItemRequest lastReq = requestItems.get(requestItems.size() - 1);
            OrderItem lastOrderItem = orderItemMap.get(lastReq.orderItemId());

            Long lastPgRefundAmount = totalPgRefundAmount - accumulatedPgItemRefund;

            items.add(RefundItem.builder()
                    .orderItem(lastOrderItem)
                    .refundQuantity(lastReq.requestQuantity())
                    .pgRefundAmount(lastPgRefundAmount)
                    .build());

        } else {
            // [분기 B 부분 환불]
            for (RefundItemRequest requestItem : requestItems) {
                items.add(allocateByRatio(requestItem, orderItemMap, payment));
            }
        }
        return items;
    }

    /**
     * 아이템 1개의 금액을 비율로 계산 (내림 처리 및 잔돈 가산)
     */
    private RefundItem allocateByRatio(RefundItemRequest requestItem, Map<Long, OrderItem> orderItemMap, Payment payment) {
        OrderItem orderItem = orderItemMap.get(requestItem.orderItemId());
        // 이번에 환불할 상품의 순수 금액 (상품 1개 가격 × 환불 수량)
        long itemTotal = orderItem.getUnitPrice() * requestItem.requestQuantity();
        long orderTotal = payment.getOrder().getTotalAmount();

        // 포인트와 PG 결제액을 '전체 결제 비율'에 맞춰 쪼갠다 (소수점 내림 처리)
        long itemPgRefundAmount = (long) Math.floor((double) itemTotal * payment.getPgAmount() / orderTotal); // PG 환불액   = 총 환불 금액 × (결제.PG 실결제 금액  / 결제.주문 총액)

        // 내림 처리 때문에 증발해버린 잔돈(1~2원)이 있는지 찾아서 PG 환불액에 가산
        long lostAmount = itemTotal - itemPgRefundAmount;
        if (lostAmount > 0) {
            itemPgRefundAmount += lostAmount;
        }

        return RefundItem.builder()
                .orderItem(orderItem)
                .refundQuantity(requestItem.requestQuantity())
                .pgRefundAmount(itemPgRefundAmount)
                .build();
    }
}
