package org.example.murderhelp.domain.chat.dto;

import jakarta.validation.constraints.NotNull;

// TODO: 인증/Member 도입 시 컨트롤러에서 Auth 정보로 대체되므로, 이 DTO 클래스는 삭제할 것
public record ChatRoomCreateRequest(
    @NotNull(message = "고객 ID는 필수입니다.")
    Long customerId
) {}
