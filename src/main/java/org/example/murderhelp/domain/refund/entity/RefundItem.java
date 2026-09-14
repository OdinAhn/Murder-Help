package org.example.murderhelp.domain.refund.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.global.entity.BaseTimeEntity;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;

@Entity
@Getter
@Table(name = "refund_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", nullable = false)
    private Refund refund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "refund_quantity", nullable = false)
    private int refundQuantity;

    @Column(name = "pg_refund_amount", nullable = false)
    private Long pgRefundAmount;

    @Builder
    private RefundItem(OrderItem orderItem, int refundQuantity, Long pgRefundAmount) {
        if (refundQuantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_QUANTITY);
        }
        
        this.orderItem = orderItem;
        this.refundQuantity = refundQuantity;
        this.pgRefundAmount = pgRefundAmount;
    }

    // 연관관계 편의 메서드
    public void assignRefund(Refund refund) {
        this.refund = refund;
    }
}
