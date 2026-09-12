package org.example.murderhelp.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.payment.dto.PaymentConfirmResponse;
import org.example.murderhelp.domain.payment.entity.FailReason;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.entity.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private final PaymentService paymentService;
    //private final CartService cartService;

    /**
     * 결제 승인 + 주문 완료
     */
    @Transactional
    public PaymentConfirmResponse approvePaymentAndOrder(Long orderId) {
        Payment payment = paymentService.findByOrderIdWithOrderForUpdate(orderId);

        // Confirm API와 Webhook이 동시에 처리된 경우
        // 먼저 완료한 요청이 있다면 나머지는 정상 종료
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentConfirmResponse.from(payment);
        }

        Order order = payment.getOrder();

        // Payment 완료
        paymentService.completePayment(payment);

        // Order 완료
        order.transitTo(OrderStatus.DELIVERED);

        // 장바구니 상품 삭제
        deleteCartItems(order);

        return PaymentConfirmResponse.from(payment);
    }

    /**
     * 사용자 취소 또는 PG 실패로 인한
     * 결제 실패 + 주문 취소
     */
    @Transactional
    public void failPaymentAndOrder(Long orderId, FailReason reason) {
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);

        Order order = payment.getOrder();

        // Payment 실패 처리
        paymentService.failPayment(payment, reason);

        // Order 취소
        order.transitTo(OrderStatus.CANCELED);

        // 재고 복구
        restoreStock(order);
    }

    /**
     * 사용자 결제 취소
     */
    @Transactional
    public void cancelPaymentAndOrder(Long orderId, FailReason reason) {
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);

        Order order = payment.getOrder();

        // Payment 취소 처리
        paymentService.cancelPayment(payment);

        // Order 취소
        order.transitTo(OrderStatus.CANCELED);

        // 재고 복구
        restoreStock(order);
    }

    /**
     * 주문 상품 재고 복구
     */
    private void restoreStock(Order order) {
        //order.getOrderItems().forEach(item -> item.getProduct().restoreStock(item.getQuantity()));
    }

    /**
     * 결제 완료 후 장바구니 상품 삭제
     */
    private void deleteCartItems(Order order) {
/*
        List<Long> productIds = order.getOrderItems()
                .stream()
                .map(OrderItem::getProduct)
                .map(product -> product.getId())
                .toList();

        cartService.deleteCartItemsByProductIds(order.getMemberId(), productIds);
 */
    }
}