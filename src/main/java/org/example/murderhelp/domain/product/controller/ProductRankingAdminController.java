package org.example.murderhelp.domain.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.product.scheduler.ProductRankingScheduler;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/product-rankings")
public class ProductRankingAdminController {

    private final ProductRankingScheduler productRankingScheduler;

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('GREEN')")
    public ApiResponse<Void> refresh() {
        productRankingScheduler.runWeeklyBestUpdate();
        return ApiResponse.ok();
    }
}
