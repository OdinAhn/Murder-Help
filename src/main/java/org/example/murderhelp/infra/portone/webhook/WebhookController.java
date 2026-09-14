package org.example.murderhelp.infra.portone.webhook;

import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PortOneWebhookVerifier portOneWebhookVerifier;
    private final WebhookHandler webhookHandler;
    private final WebhookEventService webhookEventService;

    @PostMapping("/portone")
    public ResponseEntity<ApiResponse<Void>> handlePortOneWebhook(
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestBody String body) {

        log.info("[Webhook] received id={} timestamp={}", webhookId, webhookTimestamp);

        Webhook webhook;
        try {
            webhook = portOneWebhookVerifier.verify(body, webhookId, webhookSignature, webhookTimestamp);
        } catch (WebhookVerificationException e) {
            log.warn("[Webhook] verification failed id={} reason={}", webhookId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.ok());
        }

        webhookHandler.handle(webhookId, webhook, body);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 로그인한 회원 본인의 결제/주문에 연결된 웹훅만 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<WebhookEvent>>> getWebhookEvents(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal(); // ← 실제 인증 방식에 맞게 조정 필요
        return ResponseEntity.ok(ApiResponse.ok(webhookEventService.getWebhookEventsByMember(memberId)));
    }
}