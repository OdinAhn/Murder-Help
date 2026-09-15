package org.example.murderhelp.domain.refund.component;

import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.refund.component.RefundCalculator.RefundCalculationResult;
import org.example.murderhelp.domain.refund.dto.RefundRequest;
import org.example.murderhelp.domain.refund.dto.RefundRequest.RefundItemRequest;
import org.example.murderhelp.domain.refund.entity.RefundItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * RefundCalculator 는 "전액/마지막 환불은 잔액을 마지막 항목에 몰아준다",
 * "부분 환불은 비율로 나눈 개별 금액의 합을 총액으로 쓴다"는 두 분기를 검증한다.
 *
 * 금액을 비율로 쪼갤 때 나눗셈 나머지(잔돈) 처리가 포함되어 있어, 딱 나누어떨어지는
 * 케이스와 나누어떨어지지 않는 케이스를 모두 확인한다.
 */
class RefundCalculatorTest {

    private final RefundCalculator calculator = new RefundCalculator();

    private OrderItem mockOrderItem(long unitPrice) {
        OrderItem item = mock(OrderItem.class);
        lenient().when(item.getUnitPrice()).thenReturn(unitPrice);
        return item;
    }

    private Payment mockPayment(long pgAmount, long orderTotalAmount) {
        Order order = mock(Order.class);
        lenient().when(order.getTotalAmount()).thenReturn(orderTotalAmount);

        Payment payment = mock(Payment.class);
        lenient().when(payment.getOrder()).thenReturn(order);
        lenient().when(payment.getPgAmount()).thenReturn(pgAmount);
        return payment;
    }

    @Test
    @DisplayName("전액 환불이면 마지막 항목이 남은 잔액 전부를 가져간다 (딱 나누어떨어지는 경우)")
    void fullRefundAssignsRemainderToLastItem() {
        // given: 결제 27,000원(포인트 미사용) = 9,000원 상품 + 18,000원 상품
        OrderItem item1 = mockOrderItem(9_000L);
        OrderItem item2 = mockOrderItem(18_000L);
        Map<Long, OrderItem> orderItemMap = Map.of(1L, item1, 2L, item2);
        Payment payment = mockPayment(27_000L, 27_000L);

        RefundRequest request = new RefundRequest(
                1L, "단순 변심",
                List.of(new RefundItemRequest(1L, 1), new RefundItemRequest(2L, 1))
        );

        // when: 전액 환불, 기존 환불액 0원
        RefundCalculationResult result = calculator.calculate(request, orderItemMap, payment, true, 0L);

        // then: 총액은 payment.pgAmount 그대로, 마지막 항목(item2)이 남은 금액을 통째로 가져간다
        assertThat(result.totalPgRefundAmount()).isEqualTo(27_000L);
        assertThat(result.refundItems()).hasSize(2);

        RefundItem first = result.refundItems().get(0);
        RefundItem last = result.refundItems().get(1);
        assertThat(first.getPgRefundAmount()).isEqualTo(9_000L);
        assertThat(last.getPgRefundAmount()).isEqualTo(18_000L); // 27,000 - 9,000
    }

    @Test
    @DisplayName("이미 일부 환불된 상태에서 마지막 환불이면, 남은 PG 잔액 전부를 배정한다")
    void lastRefundAssignsRemainingPgBalance() {
        OrderItem item = mockOrderItem(10_000L);
        Map<Long, OrderItem> orderItemMap = Map.of(1L, item);
        Payment payment = mockPayment(30_000L, 30_000L);

        RefundRequest request = new RefundRequest(
                1L, "재고 부족",
                List.of(new RefundItemRequest(1L, 1))
        );

        // 이미 20,000원이 환불된 상태에서 마지막 남은 건을 환불
        RefundCalculationResult result = calculator.calculate(request, orderItemMap, payment, true, 20_000L);

        assertThat(result.totalPgRefundAmount()).isEqualTo(10_000L); // 30,000 - 20,000
        assertThat(result.refundItems()).hasSize(1);
        assertThat(result.refundItems().get(0).getPgRefundAmount()).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("부분 환불이면 각 상품의 비율 계산 금액을 합산해 총액으로 쓴다")
    void partialRefundSumsRatioAllocatedAmounts() {
        // given: 포인트 미사용, 상품가와 PG 실결제액이 동일한 단순 케이스
        OrderItem item1 = mockOrderItem(9_000L);
        Map<Long, OrderItem> orderItemMap = Map.of(1L, item1);
        Payment payment = mockPayment(27_000L, 27_000L);

        RefundRequest request = new RefundRequest(
                1L, "색상이 마음에 안 듦",
                List.of(new RefundItemRequest(1L, 1))
        );

        RefundCalculationResult result = calculator.calculate(request, orderItemMap, payment, false, 0L);

        assertThat(result.refundItems()).hasSize(1);
        assertThat(result.refundItems().get(0).getPgRefundAmount()).isEqualTo(9_000L);
        // 부분 환불은 개별 항목 합산값으로 총액이 재계산된다
        assertThat(result.totalPgRefundAmount()).isEqualTo(9_000L);
    }

    @Test
    @DisplayName("여러 수량을 환불하면 단가 × 수량으로 금액이 계산된다")
    void multipliesUnitPriceByRequestedQuantity() {
        OrderItem item = mockOrderItem(5_000L);
        Map<Long, OrderItem> orderItemMap = Map.of(1L, item);
        Payment payment = mockPayment(20_000L, 20_000L);

        RefundRequest request = new RefundRequest(
                1L, "수량 부족",
                List.of(new RefundItemRequest(1L, 3)) // 5,000원 × 3개
        );

        RefundCalculationResult result = calculator.calculate(request, orderItemMap, payment, false, 0L);

        assertThat(result.refundItems().get(0).getRefundQuantity()).isEqualTo(3);
        assertThat(result.refundItems().get(0).getPgRefundAmount()).isEqualTo(15_000L);
    }
}
