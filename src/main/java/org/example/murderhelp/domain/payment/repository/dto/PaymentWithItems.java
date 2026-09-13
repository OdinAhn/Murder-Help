package org.example.murderhelp.domain.payment.repository.dto;

import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.payment.entity.Payment;

import java.util.List;

public record PaymentWithItems(Payment payment, List<OrderItem> orderItems) {}
