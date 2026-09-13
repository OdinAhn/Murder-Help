package org.example.murderhelp.domain.chat.dto;

import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.example.murderhelp.domain.chat.entity.ChatMessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long id,
    Long roomId,
    Long memberId,
    String senderEmail,
    String senderName,
    String content,
    ChatMessageType messageType,
    LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        Long memberId = chatMessage.getSender() != null ? chatMessage.getSender().getId() : null;
        String senderEmail = chatMessage.getSender() != null ? chatMessage.getSender().getEmail() : null;
        String senderName = chatMessage.getSender() != null ? chatMessage.getSender().getName() : null;

        return new ChatMessageResponse(
            chatMessage.getId(),
            chatMessage.getChatRoom().getId(),
            memberId,
            senderEmail,
            senderName,
            chatMessage.getContent(),
            chatMessage.getMessageType(),
            chatMessage.getCreatedAt()
        );
    }
}
