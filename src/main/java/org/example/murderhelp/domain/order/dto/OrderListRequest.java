package org.example.murderhelp.domain.order.dto;

import org.example.murderhelp.domain.order.entity.OrderStatus;

public record OrderListRequest(
        OrderListPeriod period,
        OrderStatus status
) {

    public OrderListRequest {
        if (period == null) {
            period = OrderListPeriod.MONTH_3; // default 값
        }
    }

}
