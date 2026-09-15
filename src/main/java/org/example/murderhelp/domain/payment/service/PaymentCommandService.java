package org.example.murderhelp.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.cart.service.CartService;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.payment.dto.PaymentConfirmResponse;
import org.example.murderhelp.domain.payment.entity.FailReason;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.entity.PaymentStatus;
import org.example.murderhelp.domain.payment.repository.dto.PaymentWithItems;
import org.example.murderhelp.domain.product.entity.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private final PaymentService paymentService;
    private final CartService cartService;

    /**
     * 결제 승인 + 주문 완료
     */
    @Transactional
    public PaymentConfirmResponse approvePaymentAndOrder(Long orderId) {
        PaymentWithItems paymentWithItems = paymentService.findByOrderIdWithOrderForUpdate(orderId);
        Payment payment = paymentWithItems.payment();
        List<OrderItem> orderItems =  paymentWithItems.orderItems();

        // Confirm API와 Webhook이 동시에 처리된 경우
        // 먼저 완료한 요청이 있다면 나머지는 정상 종료
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentConfirmResponse.from(payment);
        }

        Order order = payment.getOrder();

        // Payment 완료
        paymentService.completePayment(payment);

        // Order 완료
        order.transitTo(OrderStatus.PAID);

        // 장바구니 상품 삭제
        deleteCartItems(orderItems, order.getMember().getId());

        return PaymentConfirmResponse.from(payment);
    }

    /**
     * 사용자 취소 또는 PG 실패로 인한
     * 결제 실패 + 주문 취소
     */
    @Transactional
    public void failPaymentAndOrder(Long orderId, FailReason reason) {
        PaymentWithItems paymentWithItems = paymentService.findByOrderIdWithOrder(orderId);
        Payment payment = paymentWithItems.payment();
        List<OrderItem> orderItems =  paymentWithItems.orderItems();

        Order order = payment.getOrder();

        // Payment 실패 처리
        paymentService.failPayment(payment, reason);

        // Order 취소
        order.transitTo(OrderStatus.CANCELED);

        // 재고 복구
        restoreStock(orderItems);
    }

    /**
     * 사용자 결제 취소
     */
    @Transactional
    public void cancelPaymentAndOrder(Long orderId) {
        PaymentWithItems paymentWithItems = paymentService.findByOrderIdWithOrder(orderId);
        Payment payment = paymentWithItems.payment();
        List<OrderItem> orderItems =  paymentWithItems.orderItems();

        Order order = payment.getOrder();

        // Payment 취소 처리
        paymentService.cancelPayment(payment);

        // Order 취소
        order.transitTo(OrderStatus.CANCELED);

        // 재고 복구
        restoreStock(orderItems);
    }

    /**
     * 주문 상품 재고 복구
     */
    private void restoreStock(List<OrderItem> orderItems) {
        orderItems.forEach(item -> item.getProduct().restoreStock(item.getQuantity()));
    }

    /**
     * 결제 완료 후 장바구니 상품 삭제
     */
    private void deleteCartItems(List<OrderItem> orderItems, Long memberId) {
        List<Long> productIds = orderItems.stream()
                .map(OrderItem::getProduct)
                .map(Product::getId)
                .toList();

        cartService.deleteItemsByProductIds(memberId, productIds);   // ← 여기만 변경
    }

}