package org.example.murderhelp.domain.search.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.search.dto.PopularSearchResponse;
import org.example.murderhelp.domain.search.service.PopularSearchService;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final PopularSearchService popularSearchService;

    @GetMapping("/api/searches/popular")
    public ApiResponse<List<PopularSearchResponse>> getPopularSearches(
            @RequestParam(defaultValue = "10") @Min(1) @Max(10) int limit
    ) {
        return ApiResponse.ok(popularSearchService.getPopularSearches(limit));
    }
}
