package org.example.murderhelp.domain.refund.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.port.PaymentGateway;
import org.example.murderhelp.domain.payment.port.PaymentGatewayResponse;
import org.example.murderhelp.domain.payment.service.PaymentService;
import org.example.murderhelp.domain.refund.dto.RefundRequest;
import org.example.murderhelp.domain.refund.dto.RefundResponse;
import org.example.murderhelp.domain.refund.entity.Refund;
import org.example.murderhelp.domain.refund.service.RefundService;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundFacade {

    private final PaymentGateway paymentGateway;
    private final PaymentService paymentService;
    private final RefundService refundService;

    public RefundResponse processRefund(Long memberId, RefundRequest request) {
        log.info("========== [환불 파사드 진입] ==========");
        log.info("결제ID={}, 회원ID={}, 취소사유={}", request.paymentId(), memberId, request.cancelReason());

        /*PG 선검증*/
        Payment payment = paymentService.findByIdWithOrder(request.paymentId());

        if (payment.getPgAmount() > 0) {
            PaymentGatewayResponse pgResponse = paymentGateway.getPayment(payment.getPortonePaymentId());

            long pgRemainingAmount = pgResponse.totalAmount() - pgResponse.cancelledAmount();
            log.info("PG 환불 잔액 = {}", pgRemainingAmount);
            long dbRemainingAmount = payment.getPgAmount() - refundService.getRefundedPgAmount(payment.getId());
            log.info("DB 환불 잔액 = {}", dbRemainingAmount);

            if (pgRemainingAmount != dbRemainingAmount) {
                throw new BusinessException(ErrorCode.REFUND_AMOUNT_MISMATCH);
            }
        }

        /*선검증 및 DB 갱신*/
        Refund savedRefund = refundService.calculateAndSaveRefund(memberId, request);

        /*PG 결제 취소 호출*/
        boolean isPgSuccess = false;
        if (savedRefund.getPgRefundAmount() == 0) {
            log.info("PG 환불 금액이 0원이므로 PG사 통신을 스킵합니다. Refund ID: {}", savedRefund.getId());
            isPgSuccess = true;
        } else {
            try {
                paymentGateway.cancelPayment(
                        payment.getPortonePaymentId(),
                        request.cancelReason(),
                        savedRefund.getPgRefundAmount()
                );
                isPgSuccess = true;
                log.info("PG사 환불 통신 성공. Refund ID: {}", savedRefund.getId());
            } catch (Exception e) {
                log.error("PG사 환불 통신 실패. Refund ID: {}, Reason: {}", savedRefund.getId(), e.getMessage());
            }
        }

        /* 결과 갱신*/
        refundService.updateRefundResult(savedRefund.getId(), isPgSuccess);
        return RefundResponse.from(savedRefund);
    }

    public void syncCancelFromPg(String portonePaymentId, String reason) {
        log.info("========== [웹훅 취소 동기화 진입] ========== portonePaymentId={}", portonePaymentId);

        Payment payment = paymentService.findByPortonePaymentId(portonePaymentId);

        Refund savedRefund = refundService.calculateAndSaveFullRefundForSync(payment.getId(), reason);

        // PG 재호출 없이 바로 성공 처리 (이미 PG쪽 취소는 완료된 상태)
        refundService.updateRefundResult(savedRefund.getId(), true);

        log.info("웹훅 취소 동기화 완료. Refund ID: {}", savedRefund.getId());
    }

}