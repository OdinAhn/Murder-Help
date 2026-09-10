package org.example.murderhelp.domain.order.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public enum OrderListPeriod {

    MONTH_1(1),
    MONTH_3(3),
    MONTH_6(6),
    MONTH_12(12),
    ALL(null);

    private final Integer months;

    public LocalDateTime toLocalDateTime() {
        return months == null ? null : LocalDateTime.now().minusMonths(months);
    }

}
