package org.example.murderhelp.domain.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PREPARING_DELIVERY,
    SHIPPING,
    DELIVERED,
    CANCELED;
}
