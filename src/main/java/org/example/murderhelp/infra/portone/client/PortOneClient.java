package org.example.murderhelp.infra.portone.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.infra.portone.config.PortOneProperties;
import org.example.murderhelp.infra.portone.dto.PortOneCancelRequest;
import org.example.murderhelp.infra.portone.dto.PortOnePaymentResponse;
import org.example.murderhelp.domain.payment.port.PaymentGateway;
import org.example.murderhelp.domain.payment.port.PaymentGatewayResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortOneClient implements PaymentGateway {

    private final RestClient portOneRestClient;
    private final PortOneProperties portOneProperties;

    @Override
    public PaymentGatewayResponse getPayment(String paymentId) {
        log.info("PortOne 결제 조회: {}", paymentId);

        PortOnePaymentResponse response = portOneRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/payments/{paymentId}")
                        .queryParam("storeId", portOneProperties.getStoreId())
                        .build(paymentId))
                .retrieve()
                .body(PortOnePaymentResponse.class);

        // PortOne 응답 결과인 PortOnePaymentResponse를 PaymentGatewayResponse로 변환
        return new PaymentGatewayResponse(
                response.id(),
                response.status(),
                response.amount().total(),
                response.amount().cancelled()
        );
    }

    @Override
    public void cancelPayment(String paymentId, String reason, Integer amount) {
        String idempotencyKey = UUID.randomUUID().toString();
        log.info("PortOne 결제 취소 요청: paymentId={}, reason={}, idempotencyKey={}", paymentId, reason, idempotencyKey);

        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // portOneRestClient https://api.portone.io/payments/{paymnetId}/cancel body: {reason, storeId}
                portOneRestClient.post()
                        .uri("/payments/{paymentId}/cancel", paymentId)
                        .header("Idempotency-Key", "\"" + idempotencyKey + "\"") // 같은 키 유지
                        .body(new PortOneCancelRequest(amount, reason, portOneProperties.getStoreId()))
                        .retrieve()
                        .toBodilessEntity();
                return;  // 성공하면 종료
            } catch (ResourceAccessException e) {
                // 네트워크 타임아웃 -> 재시도
                log.warn("PortOne 취소 요청 타임아웃 (시도 {}/{})", attempt, maxRetries);
                if (attempt == maxRetries) throw e;
            }
        }
    }
}