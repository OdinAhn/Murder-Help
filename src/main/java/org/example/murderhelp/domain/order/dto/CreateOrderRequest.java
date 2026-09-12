package org.example.murderhelp.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty
        @UniqueElements
        List<@NotNull @Positive Long> cartItemIds,

        @NotBlank
        String receiverName,

        @NotBlank
        String receiverPhone,

        @NotBlank
        String deliveryAddress,

        String deliveryRequest
) {
}
