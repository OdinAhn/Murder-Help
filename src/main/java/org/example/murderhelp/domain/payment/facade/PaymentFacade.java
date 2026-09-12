package org.example.murderhelp.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.payment.dto.PaymentCancelRequest;
import org.example.murderhelp.domain.payment.dto.PaymentCancelResponse;
import org.example.murderhelp.domain.payment.dto.PaymentConfirmRequest;
import org.example.murderhelp.domain.payment.dto.PaymentConfirmResponse;
import org.example.murderhelp.domain.payment.entity.FailReason;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.entity.PaymentStatus;
import org.example.murderhelp.domain.payment.port.PaymentGateway;
import org.example.murderhelp.domain.payment.port.PaymentGatewayResponse;
import org.example.murderhelp.domain.payment.service.PaymentCommandService;
import org.example.murderhelp.domain.payment.service.PaymentService;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    private static final String PG_STATUS_PAID = "PAID";

    private final PaymentService paymentService;
    private final PaymentCommandService paymentCommandService;
    private final PaymentGateway paymentGateway;

    /**
     * 결제 승인
     */
    public PaymentConfirmResponse confirmPayment(Long memberId, PaymentConfirmRequest request) {
        Payment payment = paymentService.findByOrderIdWithOrder(request.orderId());

        Order order = payment.getOrder();

        // 주문 소유자 검증
        validateMember(memberId, order);

        // 이미 처리된 결제인지 검증
        validatePaymentStatus(payment);

        // 요청의 PortOne Payment ID와 DB의 Payment ID가 같은지 검증
        validatePortonePaymentId(payment, request);

        // 전액 포인트 결제라면 PG 조회를 하지 않는다.
        if (payment.getPgAmount() == 0) {
            log.info("전액 포인트 결제 — PG 조회 스킵: paymentId={}", payment.getId());

            return paymentCommandService.approvePaymentAndOrder(order.getId());
        }

        // PortOne 결제 정보 조회
        PaymentGatewayResponse pgPayment = paymentGateway.getPayment(payment.getPortonePaymentId());

        // PG 결제 상태 검증
        if (!PG_STATUS_PAID.equals(pgPayment.status())) {
            log.error("결제 승인 실패 — PG 상태 비정상: paymentId={}, pgStatus={}", payment.getId(), pgPayment.status());

            paymentCommandService.failPaymentAndOrder(order.getId(), FailReason.PG_DECLINED);

            throw new BusinessException(ErrorCode.PAYMENT_NOT_PAID
            );
        }

        // 결제 금액 검증
        if (payment.getPgAmount() != pgPayment.totalAmount()) {
            log.error("결제 승인 실패 — 금액 불일치: paymentId={}, DB pgAmount={}, PG금액={}", payment.getId(), payment.getPgAmount(), pgPayment.totalAmount());

            try {
                paymentGateway.cancelPayment(payment.getPortonePaymentId(), "결제 금액 불일치 자동 취소", null);
            } catch (Exception e) {
                log.error("PG 자동 취소 실패 : 수동 처리 필요: portonePaymentId={}", payment.getPortonePaymentId(), e);
            }

            paymentCommandService.failPaymentAndOrder(order.getId(), FailReason.AMOUNT_MISMATCH);

            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 모든 외부 결제 검증이 끝났으므로
        // 실제 내부 결제 승인 처리를 CommandService에 위임
        return paymentCommandService.approvePaymentAndOrder(order.getId());
    }

    /**
     * 사용자 결제 취소
     */
    public PaymentCancelResponse cancelPayment(Long memberId, Long paymentId, PaymentCancelRequest request) {
        Payment payment = paymentService.findByIdWithOrder(paymentId);

        Order order = payment.getOrder();

        // 주문 소유자 검증
        validateMember(memberId, order);

        // 실제 결제 취소 + 주문 취소 처리
        paymentCommandService.cancelPaymentAndOrder(order.getId(), FailReason.USER_CANCELLED);

        // 변경된 Payment 조회
        Payment updatedPayment = paymentService.findByIdWithOrder(paymentId);

        String message = extractCancelMessage(request);

        return PaymentCancelResponse.from(updatedPayment, message);
    }

    /**
     * 결제 실패 처리
     */
    public PaymentCancelResponse failPayment(Long paymentId, FailReason reason) {
        Payment payment = paymentService.findByIdWithOrder(paymentId);

        paymentCommandService.failPaymentAndOrder(payment.getOrder().getId(), reason);

        Payment updatedPayment = paymentService.findByIdWithOrder(paymentId);

        return PaymentCancelResponse.from(updatedPayment, "결제가 실패 처리되었습니다.");
    }

    /**
     * 주문 소유자 검증
     */
    private void validateMember(Long memberId, Order order) {
        // TODO : ORDER에 MEMBER 연동 되면 주석 해제
        /*if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE
            );
        }*/
    }

    /**
     * 결제 상태 검증
     */
    private void validatePaymentStatus(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT
            );
        }
    }

    /**
     * PortOne Payment ID 검증
     */
    private void validatePortonePaymentId(Payment payment, PaymentConfirmRequest request) {
        String portonePaymentId = payment.getPortonePaymentId();

        if (!portonePaymentId.equals(request.portonePaymentId())) {
            log.warn(
                    "결제 승인 거부 — portonePaymentId 불일치: DB={}, 요청={}",
                    portonePaymentId,
                    request.portonePaymentId()
            );

            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE
            );
        }
    }

    /**
     * 결제 취소 메시지 생성
     */
    private String extractCancelMessage(PaymentCancelRequest request) {
        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            return request.reason();
        }
        return "결제가 취소되었습니다.";
    }







}
