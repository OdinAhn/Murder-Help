package org.example.murderhelp.infra.portone.webhook;

import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransactionCancelledCancelled;
import io.portone.sdk.server.webhook.WebhookTransactionCancelledPartialCancelled;
import io.portone.sdk.server.webhook.WebhookTransactionPaid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.entity.PaymentStatus;
import org.example.murderhelp.domain.payment.port.PaymentGateway;
import org.example.murderhelp.domain.payment.port.PaymentGatewayResponse;
import org.example.murderhelp.domain.payment.service.PaymentCommandService;
import org.example.murderhelp.domain.payment.service.PaymentService;
import org.example.murderhelp.domain.refund.facade.RefundFacade;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookHandler {

    private final PaymentService paymentService;
    private final PaymentCommandService paymentCommandService;
    private final PaymentGateway paymentGateway;
    private final WebhookEventService webhookEventService;
    private final RefundFacade refundFacade;

    public void handle(String webhookId, Webhook webhook, String rawPayload) {

        String type = webhook.getClass().getSimpleName();

        Optional<WebhookEvent> saved = webhookEventService.saveIfNotDuplicate(webhookId, type, rawPayload);
        if (saved.isEmpty()) return;
        Long eventId = saved.get().getId();

        try {
            if (webhook instanceof WebhookTransactionPaid p) {
                handlePaid(eventId, p.getData().getPaymentId());
            } else if (webhook instanceof WebhookTransactionCancelledCancelled c) {
                handleCancel(eventId, c.getData().getPaymentId());
            } else if (webhook instanceof WebhookTransactionCancelledPartialCancelled pc) {  
                handlePartialCancel(eventId, pc.getData().getPaymentId());
            } else {
                webhookEventService.markIgnored(eventId, "처리 대상 아님: " + type);
            }
        } catch (Exception e) {
            log.error("[Webhook] failed eventId={}", eventId, e);
            webhookEventService.markFailed(eventId, e.getMessage());
        }
    }

    private void handlePartialCancel(
            Long eventId,
            String portonePaymentId
    ) {
        PaymentGatewayResponse pg =
                paymentGateway.getPayment(portonePaymentId);

        Payment payment =
                paymentService.findByPortonePaymentId(portonePaymentId);

        webhookEventService.attachPaymentId(
                eventId,
                payment.getId()
        );

        log.info(
                "[Webhook] 부분 취소 확인. paymentId={}, cancelledAmount={}",
                payment.getId(),
                pg.cancelledAmount()
        );

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            payment.partialRefund();
        }

        webhookEventService.markProcessed(eventId);

        log.info(
                "[Webhook] 부분 취소 동기화 완료. paymentId={}, cancelledAmount={}",
                payment.getId(),
                pg.cancelledAmount()
        );
    }

    private void handlePaid(Long eventId, String portonePaymentId) {
        PaymentGatewayResponse pg = paymentGateway.getPayment(portonePaymentId);

        if (!"PAID".equals(pg.status())) {
            webhookEventService.markIgnored(eventId, "PG 상태가 PAID가 아님: " + pg.status());
            return;
        }

        Payment payment = paymentService.findByPortonePaymentId(portonePaymentId);

        // 이 웹훅 이벤트를 어떤 결제와 연결된 것인지 기록 (회원별 웹훅 조회에 사용)
        webhookEventService.attachPaymentId(eventId, payment.getId());

        if (pg.totalAmount() != payment.getPgAmount()) {
            webhookEventService.markFailed(
                    eventId, "금액 불일치: db=" + payment.getPgAmount() + ", pg=" + pg.totalAmount()
            );
            return;
        }

        if (payment.getStatus() == PaymentStatus.PENDING) {
            paymentCommandService.approvePaymentAndOrder(payment.getOrder().getId());
        }

        webhookEventService.markProcessed(eventId);
    }

    /**
     * 결제 취소(Transaction.Cancelled) 웹훅 처리
     */
    private void handleCancel(Long eventId, String portonePaymentId) {
        PaymentGatewayResponse pg = paymentGateway.getPayment(portonePaymentId);

        if (!"CANCELLED".equals(pg.status())) {
            webhookEventService.markIgnored(eventId, "PG 상태가 CANCELLED가 아님: " + pg.status());
            return;
        }

        Payment payment = paymentService.findByPortonePaymentId(portonePaymentId);

        // 이 웹훅 이벤트를 어떤 결제와 연결된 것인지 기록 (회원별 웹훅 조회에 사용)
        webhookEventService.attachPaymentId(eventId, payment.getId());

        // 이미 우리 쪽에서 처리된 취소(COMPLETED/PARTIAL_REFUND가 아닌 경우)면 스킵 — 중복 처리 방지
        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.PARTIAL_REFUND) {
            refundFacade.syncCancelFromPg(portonePaymentId, "PG사 상태 동기화에 의한 취소");
        }

        webhookEventService.markProcessed(eventId);
    }

    
}