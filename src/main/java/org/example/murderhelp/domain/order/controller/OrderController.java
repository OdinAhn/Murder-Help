package org.example.murderhelp.domain.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.order.dto.OrderListRequest;
import org.example.murderhelp.domain.order.dto.OrderResponse;
import org.example.murderhelp.domain.order.service.OrderService;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<Page<OrderResponse>> getOrderList(
            @AuthenticationPrincipal Long memberId,
            @Valid OrderListRequest orderListRequest,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ApiResponse.ok(orderService.getOrderList(memberId, orderListRequest, pageable));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long orderId
    ) {
        return ApiResponse.ok(orderService.getOrder(memberId, orderId));
    }

}
