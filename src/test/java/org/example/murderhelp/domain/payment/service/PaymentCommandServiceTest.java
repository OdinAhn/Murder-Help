package org.example.murderhelp.domain.payment.service;

import org.example.murderhelp.domain.cart.service.CartService;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.payment.entity.FailReason;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.entity.PaymentStatus;
import org.example.murderhelp.domain.payment.repository.dto.PaymentWithItems;
import org.example.murderhelp.domain.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PaymentCommandService 는 "결제 상태 변경"과 "주문 상태 변경"을 하나의 트랜잭션으로 묶는
 * 곳이라, 여기서 재고 복구·장바구니 정리·상태 전이가 실제로 함께 일어나는지가 핵심이다.
 *
 * 특히 approvePaymentAndOrder() 는 과거 실제로 Order.transitTo(DELIVERED) 로 잘못
 * 전이시키던 버그가 있었던 지점이라, 정확히 PAID 로 전이하는지를 명시적으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private CartService cartService;

    @InjectMocks
    private PaymentCommandService paymentCommandService;

    private Member member;
    private Order order;
    private Payment payment;
    private Product product;
    private OrderItem orderItem;

    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long PRODUCT_ID = 50L;

    @BeforeEach
    void setUp() {
        member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(MEMBER_ID);

        order = mock(Order.class);
        lenient().when(order.getId()).thenReturn(ORDER_ID);
        lenient().when(order.getMember()).thenReturn(member);
        lenient().when(order.getStatus())
                .thenReturn(org.example.murderhelp.domain.order.entity.OrderStatus.PENDING_PAYMENT);   // 이 줄 추가

        product = mock(Product.class);
        lenient().when(product.getId()).thenReturn(PRODUCT_ID);

        orderItem = mock(OrderItem.class);
        lenient().when(orderItem.getProduct()).thenReturn(product);
        lenient().when(orderItem.getQuantity()).thenReturn(2);

        payment = mock(Payment.class);
        lenient().when(payment.getOrder()).thenReturn(order);
    }

    @Nested
    @DisplayName("approvePaymentAndOrder")
    class ApprovePaymentAndOrder {

        @Test
        @DisplayName("결제를 완료 처리하고 주문을 PAID 로 전이시킨다 (DELIVERED 아님)")
        void completesPaymentAndTransitsOrderToPaid() {
            when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of(orderItem));
            when(paymentService.findByOrderIdWithOrderForUpdate(ORDER_ID)).thenReturn(paymentWithItems);

            paymentCommandService.approvePaymentAndOrder(ORDER_ID);

            verify(paymentService).completePayment(payment);
            verify(order).transitTo(OrderStatus.PAID);
            verify(order, never()).transitTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("결제 완료 후 주문 상품을 productId 기준으로 장바구니에서 정리한다")
        void deletesCartItemsByProductIdAfterApproval() {
            when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of(orderItem));
            when(paymentService.findByOrderIdWithOrderForUpdate(ORDER_ID)).thenReturn(paymentWithItems);

            paymentCommandService.approvePaymentAndOrder(ORDER_ID);

            // cartItemId 가 아니라 productId 기준 정리 메서드가 호출되어야 한다
            verify(cartService).deleteItemsByProductIds(MEMBER_ID, List.of(PRODUCT_ID));
        }

        @Test
        @DisplayName("이미 COMPLETED 상태면 중복 처리하지 않고 즉시 응답만 반환한다")
        void isIdempotentWhenAlreadyCompleted() {
            when(payment.getStatus()).thenReturn(PaymentStatus.COMPLETED);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of(orderItem));
            when(paymentService.findByOrderIdWithOrderForUpdate(ORDER_ID)).thenReturn(paymentWithItems);

            paymentCommandService.approvePaymentAndOrder(ORDER_ID);

            // Confirm API와 웹훅이 동시에 들어와도 두 번째 요청은 아무 것도 바꾸면 안 된다
            verify(paymentService, never()).completePayment(any());
            verify(order, never()).transitTo(any());
            verify(cartService, never()).deleteItemsByProductIds(anyLong(), anyList());
        }
    }

    @Nested
    @DisplayName("failPaymentAndOrder")
    class FailPaymentAndOrder {

        @Test
        @DisplayName("결제를 실패 처리하고 주문을 취소하며 재고를 복구한다")
        void failsPaymentCancelsOrderAndRestoresStock() {
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of(orderItem));
            when(paymentService.findByOrderIdWithOrder(ORDER_ID)).thenReturn(paymentWithItems);

            paymentCommandService.failPaymentAndOrder(ORDER_ID, FailReason.PG_DECLINED);

            verify(paymentService).failPayment(payment, FailReason.PG_DECLINED);
            verify(order).transitTo(OrderStatus.CANCELED);
            verify(product).restoreStock(2);
        }
    }

    @Nested
    @DisplayName("cancelPaymentAndOrder")
    class CancelPaymentAndOrder {

        @Test
        @DisplayName("결제를 취소 처리하고 주문을 취소하며 재고를 복구한다")
        void cancelsPaymentCancelsOrderAndRestoresStock() {
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of(orderItem));
            when(paymentService.findByOrderIdWithOrder(ORDER_ID)).thenReturn(paymentWithItems);

            paymentCommandService.cancelPaymentAndOrder(ORDER_ID);

            verify(paymentService).cancelPayment(payment);
            verify(order).transitTo(OrderStatus.CANCELED);
            verify(product).restoreStock(2);
        }
    }
}
