package org.example.murderhelp.domain.payment.facade;

import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.payment.dto.PaymentCancelRequest;
import org.example.murderhelp.domain.payment.dto.PaymentConfirmRequest;
import org.example.murderhelp.domain.payment.dto.PaymentConfirmResponse;
import org.example.murderhelp.domain.payment.entity.FailReason;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.port.PaymentGateway;
import org.example.murderhelp.domain.payment.port.PaymentGatewayResponse;
import org.example.murderhelp.domain.payment.repository.dto.PaymentWithItems;
import org.example.murderhelp.domain.payment.service.PaymentCommandService;
import org.example.murderhelp.domain.payment.service.PaymentService;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PaymentFacade.confirmPayment() 의 핵심 분기(정상 승인, PG 미승인, 금액 불일치,
 * 이미 처리된 결제, portonePaymentId 불일치, 소유자 불일치, 전액 포인트 결제)를 검증한다.
 *
 * 원칙: 이 클래스는 "포트원 API 응답을 절대 신뢰하지 말고 서버가 재검증한다"는
 * 설계 원칙이 실제로 지켜지는지 확인하는 게 목적이다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentCommandService paymentCommandService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentFacade paymentFacade;

    private Member member;
    private Order order;

    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long PAYMENT_ID = 200L;
    private static final String PORTONE_PAYMENT_ID = "pay_abc-123";

    @BeforeEach
    void setUp() {
        member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(MEMBER_ID);

        order = mock(Order.class);
        lenient().when(order.getId()).thenReturn(ORDER_ID);
        lenient().when(order.getMember()).thenReturn(member);
        lenient().when(order.getStatus())
                .thenReturn(org.example.murderhelp.domain.order.entity.OrderStatus.PAID);   // 이 줄 추가
    }

    private Payment newPayment(long amount, long pgAmount) {
        Payment payment = mock(Payment.class);
        lenient().when(payment.getId()).thenReturn(PAYMENT_ID);
        lenient().when(payment.getOrder()).thenReturn(order);
        lenient().when(payment.getPortonePaymentId()).thenReturn(PORTONE_PAYMENT_ID);
        lenient().when(payment.getAmount()).thenReturn(amount);
        lenient().when(payment.getPgAmount()).thenReturn(pgAmount);
        lenient().when(payment.getStatus())
                .thenReturn(org.example.murderhelp.domain.payment.entity.PaymentStatus.PENDING);
        return payment;
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        @Test
        @DisplayName("PG 상태가 PAID이고 금액이 일치하면 결제를 승인 처리한다")
        void confirmsWhenPgPaidAndAmountMatches() {
            Payment payment = newPayment(30_000L, 30_000L);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of());
            PaymentConfirmRequest request = new PaymentConfirmRequest(ORDER_ID, PORTONE_PAYMENT_ID);

            when(paymentService.findByOrderIdWithOrder(ORDER_ID)).thenReturn(paymentWithItems);
            when(paymentGateway.getPayment(PORTONE_PAYMENT_ID))
                    .thenReturn(new PaymentGatewayResponse(PORTONE_PAYMENT_ID, "PAID", 30_000L, 0L));

            PaymentConfirmResponse expected = mock(PaymentConfirmResponse.class);
            when(paymentCommandService.approvePaymentAndOrder(ORDER_ID)).thenReturn(expected);

            PaymentConfirmResponse result = paymentFacade.confirmPayment(MEMBER_ID, request);

            assertThat(result).isEqualTo(expected);
            verify(paymentCommandService).approvePaymentAndOrder(ORDER_ID);
            verify(paymentCommandService, never()).failPaymentAndOrder(anyLong(), any());
            verify(paymentGateway, never()).cancelPayment(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("전액 포인트 결제(pgAmount=0)면 PG 조회 없이 바로 승인 처리한다")
        void skipsPgLookupWhenFullyPaidByPoints() {
            Payment payment = newPayment(30_000L, 0L);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of());
            PaymentConfirmRequest request = new PaymentConfirmRequest(ORDER_ID, PORTONE_PAYMENT_ID);

            when(paymentService.findByOrderIdWithOrder(ORDER_ID)).thenReturn(paymentWithItems);
            PaymentConfirmResponse expected = mock(PaymentConfirmResponse.class);
            when(paymentCommandService.approvePaymentAndOrder(ORDER_ID)).thenReturn(expected);

            PaymentConfirmResponse result = paymentFacade.confirmPayment(MEMBER_ID, request);

            assertThat(result).isEqualTo(expected);
            verify(paymentGateway, never()).getPayment(anyString());
            verify(paymentCommandService).approvePaymentAndOrder(ORDER_ID);
        }

        @Test
        @DisplayName("PG 상태가 PAID가 아니면 결제를 실패 처리하고 예외를 던진다")
        void failsWhenPgStatusIsNotPaid() {
            Payment payment = newPayment(30_000L, 30_000L);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of());
            PaymentConfirmRequest request = new PaymentConfirmRequest(ORDER_ID, PORTONE_PAYMENT_ID);

            when(paymentService.findByOrderIdWithOrder(ORDER_ID)).thenReturn(paymentWithItems);
            when(paymentGateway.getPayment(PORTONE_PAYMENT_ID))
                    .thenReturn(new PaymentGatewayResponse(PORTONE_PAYMENT_ID, "FAILED", 30_000L, 0L));

            assertThatThrownBy(() -> paymentFacade.confirmPayment(MEMBER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PAYMENT_NOT_PAID);

            verify(paymentCommandService).failPaymentAndOrder(ORDER_ID, FailReason.PG_DECLINED);
            verify(paymentCommandService, never()).approvePaymentAndOrder(anyLong());
        }

        @Test
        @DisplayName("결제 금액이 불일치하면 PG 취소를 시도하고 실패 처리 후 예외를 던진다")
        void failsAndAutoCancelsWhenAmountMismatches() {
            Payment payment = newPayment(30_000L, 30_000L);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of());
            PaymentConfirmRequest request = new PaymentConfirmRequest(ORDER_ID, PORTONE_PAYMENT_ID);

            when(paymentService.findByOrderIdWithOrder(ORDER_ID)).thenReturn(paymentWithItems);
            // PG가 실제로는 더 적은 금액(위변조 의심)으로 응답
            when(paymentGateway.getPayment(PORTONE_PAYMENT_ID))
                    .thenReturn(new PaymentGatewayResponse(PORTONE_PAYMENT_ID, "PAID", 1_000L, 0L));

            assertThatThrownBy(() -> paymentFacade.confirmPayment(MEMBER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

            verify(paymentGateway).cancelPayment(eq(PORTONE_PAYMENT_ID), anyString(), isNull());
            verify(paymentCommandService).failPaymentAndOrder(ORDER_ID, FailReason.AMOUNT_MISMATCH);
        }

        @Test
        @DisplayName("PG 자동 취소 API 호출이 실패해도 승인 실패 처리는 계속 진행된다")
        void continuesFailureHandlingEvenIfAutoCancelThrows() {
            Payment payment = newPayment(30_000L, 30_000L);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of());
            PaymentConfirmRequest request = new PaymentConfirmRequest(ORDER_ID, PORTONE_PAYMENT_ID);

            when(paymentService.findByOrderIdWithOrder(ORDER_ID)).thenReturn(paymentWithItems);
            when(paymentGateway.getPayment(PORTONE_PAYMENT_ID))
                    .thenReturn(new PaymentGatewayResponse(PORTONE_PAYMENT_ID, "PAID", 1_000L, 0L));
            doThrow(new RuntimeException("PG 통신 장애"))
                    .when(paymentGateway).cancelPayment(anyString(), anyString(), any());

            assertThatThrownBy(() -> paymentFacade.confirmPayment(MEMBER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

            // PG 취소가 실패했어도 우리 쪽 실패 처리(주문 취소 등)는 반드시 수행되어야 한다
            verify(paymentCommandService).failPaymentAndOrder(ORDER_ID, FailReason.AMOUNT_MISMATCH);
        }

        @Test
        @DisplayName("이미 처리된 결제(PENDING이 아님)면 PG 조회 없이 즉시 예외를 던진다")
        void rejectsAlreadyProcessedPayment() {
            Payment payment = newPayment(30_000L, 30_000L);
            when(payment.getStatus())
                    .thenReturn(org.example.murderhelp.domain.payment.entity.PaymentStatus.COMPLETED);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of());
            PaymentConfirmRequest request = new PaymentConfirmRequest(ORDER_ID, PORTONE_PAYMENT_ID);

            when(paymentService.findByOrderIdWithOrder(ORDER_ID)).thenReturn(paymentWithItems);

            assertThatThrownBy(() -> paymentFacade.confirmPayment(MEMBER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_PROCESSED_PAYMENT);

            verify(paymentGateway, never()).getPayment(anyString());
        }

        @Test
        @DisplayName("요청의 portonePaymentId가 DB와 다르면 예외를 던진다")
        void rejectsMismatchedPortonePaymentId() {
            Payment payment = newPayment(30_000L, 30_000L);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of());
            PaymentConfirmRequest request = new PaymentConfirmRequest(ORDER_ID, "다른-포트원-ID");

            when(paymentService.findByOrderIdWithOrder(ORDER_ID)).thenReturn(paymentWithItems);

            assertThatThrownBy(() -> paymentFacade.confirmPayment(MEMBER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

            verify(paymentGateway, never()).getPayment(anyString());
        }

        @Test
        @DisplayName("주문 소유자가 아니면 예외를 던진다")
        void rejectsWhenMemberIsNotOwner() {
            Payment payment = newPayment(30_000L, 30_000L);
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of());
            PaymentConfirmRequest request = new PaymentConfirmRequest(ORDER_ID, PORTONE_PAYMENT_ID);

            when(paymentService.findByOrderIdWithOrder(ORDER_ID)).thenReturn(paymentWithItems);

            Long otherMemberId = 999L;

            assertThatThrownBy(() -> paymentFacade.confirmPayment(otherMemberId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("소유자가 맞으면 결제를 취소하고 갱신된 상태를 반환한다")
        void cancelsPaymentSuccessfully() {
            Payment pendingPayment = newPayment(30_000L, 30_000L);
            Payment cancelledPayment = newPayment(30_000L, 30_000L);
            when(cancelledPayment.getStatus())
                    .thenReturn(org.example.murderhelp.domain.payment.entity.PaymentStatus.CANCELLED);

            when(paymentService.findByIdWithOrder(PAYMENT_ID))
                    .thenReturn(pendingPayment)
                    .thenReturn(cancelledPayment);

            var response = paymentFacade.cancelPayment(
                    MEMBER_ID, PAYMENT_ID, new PaymentCancelRequest("단순 변심"));

            assertThat(response.paymentStatus()).isEqualTo("CANCELLED");
            assertThat(response.message()).isEqualTo("단순 변심");
            verify(paymentCommandService).cancelPaymentAndOrder(ORDER_ID);
        }

        @Test
        @DisplayName("취소 사유를 입력하지 않으면 기본 메시지를 사용한다")
        void usesDefaultMessageWhenReasonIsBlank() {
            Payment payment = newPayment(30_000L, 30_000L);
            when(paymentService.findByIdWithOrder(PAYMENT_ID)).thenReturn(payment);

            var response = paymentFacade.cancelPayment(MEMBER_ID, PAYMENT_ID, null);

            assertThat(response.message()).isEqualTo("결제가 취소되었습니다.");
        }

        @Test
        @DisplayName("소유자가 아니면 취소할 수 없다")
        void rejectsWhenMemberIsNotOwner() {
            Payment payment = newPayment(30_000L, 30_000L);
            when(paymentService.findByIdWithOrder(PAYMENT_ID)).thenReturn(payment);

            assertThatThrownBy(() -> paymentFacade.cancelPayment(999L, PAYMENT_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

            verify(paymentCommandService, never()).cancelPaymentAndOrder(anyLong());
        }
    }
}
