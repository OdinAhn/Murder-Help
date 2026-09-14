package org.example.murderhelp.domain.refund.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.refund.dto.RefundHistoryResponse;
import org.example.murderhelp.domain.refund.dto.RefundRequest;
import org.example.murderhelp.domain.refund.dto.RefundResponse;
import org.example.murderhelp.domain.refund.facade.RefundFacade;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import org.example.murderhelp.domain.refund.dto.RefundableItemResponse;
import org.example.murderhelp.domain.refund.service.RefundService;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundFacade refundFacade;
    private final RefundService refundService;

    /**
     * 환불 요청 API
     * 인증된 memberId를 함께 전달
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RefundResponse>> processRefund(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RefundRequest request) {
        RefundResponse response = refundFacade.processRefund(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 환불 가능 아이템 조회 API
     * 인증된 memberId를 함께 전달
     */
    @GetMapping("/refundable/{paymentId}")
    public ResponseEntity<ApiResponse<List<RefundableItemResponse>>> getRefundableItems(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.getRefundableItems(memberId, paymentId)));
    }

    /**
     * 환불 영수증 조회 API
     * 인증된 memberId를 함께 전달
     */
    @GetMapping("/history/{paymentId}")
    public ResponseEntity<ApiResponse<List<RefundHistoryResponse>>> getRefundHistory(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.getRefundHistory(memberId, paymentId)));
    }
}
