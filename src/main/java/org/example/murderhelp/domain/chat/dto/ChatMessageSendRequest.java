package org.example.murderhelp.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.murderhelp.domain.chat.entity.ChatMessageType;

public record ChatMessageSendRequest(
    @NotNull(message = "채팅방 ID는 필수입니다.")
    Long roomId,
    
    @NotNull(message = "발신자 ID는 필수입니다.")
    Long memberId, // TODO: 인증/Member 도입 시 세션/토큰 정보로 대체 고려
    
    @NotBlank(message = "메시지 내용은 필수입니다.")
    String content,

    ChatMessageType messageType
) {}
