package org.example.murderhelp.domain.chat.dto;

import org.example.murderhelp.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long id,
    Long roomId,
    Long memberId,
    String content,
    LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
            chatMessage.getId(),
            chatMessage.getChatRoom().getId(),
            chatMessage.getMemberId(),
            chatMessage.getContent(),
            chatMessage.getCreatedAt()
        );
    }
}
