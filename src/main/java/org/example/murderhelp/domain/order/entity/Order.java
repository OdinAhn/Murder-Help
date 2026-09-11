package org.example.murderhelp.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.global.entity.BaseTimeEntity;

@Getter
@Entity
@Table(name = "orders", uniqueConstraints = {@UniqueConstraint(name = "uk_orders_order_number",
        columnNames = {"order_number"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Size(max = 50)
    @NotNull
    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @NotNull
    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Size(max = 50)
    @NotNull
    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Size(max = 20)
    @NotNull
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @Size(max = 255)
    @NotNull
    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Size(max = 255)
    @Column(name = "delivery_request")
    private String deliveryRequest;

    @Builder
    public Order(
            String orderNumber,
            Long totalAmount,
            String receiverName,
            String receiverPhone,
            String deliveryAddress,
            String deliveryRequest
    ) {
        this.orderNumber = orderNumber;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.totalAmount = totalAmount;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.deliveryAddress = deliveryAddress;
        this.deliveryRequest = deliveryRequest;
    }

}