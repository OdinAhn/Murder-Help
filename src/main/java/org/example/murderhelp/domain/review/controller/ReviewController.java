package org.example.murderhelp.domain.review.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.review.dto.ReviewCreateRequest;
import org.example.murderhelp.domain.review.dto.ReviewResponse;
import org.example.murderhelp.domain.review.dto.ReviewUpdateRequest;
import org.example.murderhelp.domain.review.service.ReviewService;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/me")
    public ApiResponse<List<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal Long memberId
    ) {
        return ApiResponse.ok(reviewService.getMyReviews(memberId));
    }

    @PostMapping
    public ApiResponse<ReviewResponse> createReview(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid ReviewCreateRequest request
    ) {
        return ApiResponse.ok(reviewService.createReview(memberId, request));
    }

    @PatchMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long reviewId,
            @RequestBody @Valid ReviewUpdateRequest request
    ) {
        return ApiResponse.ok(
                reviewService.updateReview(memberId, reviewId, request)
        );
    }

    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> deleteReview(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(memberId, reviewId);
        return ApiResponse.ok();
    }
}