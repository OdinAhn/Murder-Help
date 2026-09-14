package org.example.murderhelp.domain.refund.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.global.entity.BaseTimeEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "cancel_reason", nullable = false)
    private String cancelReason;

    @Column(name = "pg_refund_amount", nullable = false)
    private Long pgRefundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    // 빌더 패턴 적용
    @Builder
    private Refund(Payment payment, String cancelReason, Long pgRefundAmount) {
        this.payment = payment;
        this.cancelReason = cancelReason;
        this.pgRefundAmount = pgRefundAmount;
        this.status = RefundStatus.COMPLETED; // 기본 생성 상태값
    }

    // 비즈니스 로직 캡슐화 (상태 변경 - Enum에 위임)
    public void changeStatus(RefundStatus newStatus) {
        // Enum 내부에 정의된 상태 전이 검증 로직 호출
        this.status.validateTransitionTo(newStatus);
        this.status = newStatus;
    }
}
