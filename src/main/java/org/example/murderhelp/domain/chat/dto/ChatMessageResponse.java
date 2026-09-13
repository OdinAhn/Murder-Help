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
    String senderGrade,
    String content,
    ChatMessageType messageType,
    LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        Long memberId = chatMessage.getSender().getId();
        String senderEmail = chatMessage.getSender().getEmail();
        String senderName = chatMessage.getSender().getName();
        String senderGrade = chatMessage.getSender().getGrade().name();

        return new ChatMessageResponse(
            chatMessage.getId(),
            chatMessage.getChatRoom().getId(),
            memberId,
            senderEmail,
            senderName,
            senderGrade,
            chatMessage.getContent(),
            chatMessage.getMessageType(),
            chatMessage.getCreatedAt()
        );
    }
}
