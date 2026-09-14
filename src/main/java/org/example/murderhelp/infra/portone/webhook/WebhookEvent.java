package org.example.murderhelp.infra.portone.webhook;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.global.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events")
@Getter
@NoArgsConstructor
public class WebhookEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "webhook_id", nullable = false, unique = true, length = 200)
    private String webhookId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 30)
    private WebhookStatus status;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    public WebhookEvent(Long paymentId, String webhookId, String eventType, String payload) {
        this.paymentId = paymentId;
        this.webhookId = webhookId;
        this.eventType = eventType;
        this.status = WebhookStatus.RECEIVED;
        this.payload = payload;
    }

    public void markAsProcessed() {
        this.status = WebhookStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.failReason = null;
    }

    public void markAsIgnored(String reason) {
        this.status = WebhookStatus.IGNORED;
        this.processedAt = LocalDateTime.now();
        this.failReason = reason;
    }

    public void markAsFailed(String reason) {
        this.status = WebhookStatus.FAILED;
        this.processedAt = LocalDateTime.now();
        this.failReason = reason;
    }
    
    public void assignPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }
}