package org.example.murderhelp.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.global.entity.BaseTimeEntity;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "portone_payment_id", nullable = false, length = 200, unique = true)
    private String portonePaymentId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "pg_amount", nullable = false)
    private int pgAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_reason", length = 50)
    private FailReason failReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    private Payment(Order order, int amount, int pointUsedAmount) {
        // 1. 음수 방지
        if (amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        this.order = order;
        this.portonePaymentId = generatePortonePaymentId();
        this.amount = amount;
        this.pgAmount = amount - pointUsedAmount;
        this.status = PaymentStatus.PENDING;
    }

    private static String generatePortonePaymentId() {
        return "pay_" + UUID.randomUUID();
    }

    public void complete() {
        changeStatus(PaymentStatus.COMPLETED);
        this.paidAt = LocalDateTime.now();
    }

    public void fail(FailReason reason) {
        changeStatus(PaymentStatus.FAILED);
        this.failReason = reason;
    }

    public void cancel() {
        // 2. 중복 세팅 제거 (changeStatus 내부에서 처리함)
        changeStatus(PaymentStatus.CANCELLED);
    }

    public void validateRefundable() {
        if (this.status != PaymentStatus.COMPLETED && this.status != PaymentStatus.PARTIAL_REFUND) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_STATUS);
        }
    }

    public void fullRefund() {
        changeStatus(PaymentStatus.FULL_REFUND);
    }

    public void partialRefund() {
        changeStatus(PaymentStatus.PARTIAL_REFUND);
    }

    private void changeStatus(PaymentStatus target) {
        if (!this.status.canTransitTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.status = target;
    }
}
