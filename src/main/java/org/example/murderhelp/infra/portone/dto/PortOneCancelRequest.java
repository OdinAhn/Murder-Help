package org.example.murderhelp.infra.portone.dto;

public record PortOneCancelRequest(
        Integer amount,  // 취소 총 금액 (값을 입력하지 않으면 전액 취소됩니다.)
        String reason,   // [필수] 취소 사유
        String storeId   // [조건부] 하위 상점 사용 시 필수
) {}