package org.example.murderhelp.domain.cart.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.cart.dto.CartItemAddRequest;
import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.cart.service.CartService;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart/items")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ApiResponse<CartItemResponse> addItem(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        return ApiResponse.ok(cartService.addItem(memberId, request));
    }
}
