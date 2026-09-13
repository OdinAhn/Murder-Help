package org.example.murderhelp.domain.chat.event;

import org.example.murderhelp.domain.chat.entity.ChatMessage;

public record ChatMessageCreatedEvent(
        Long roomId,
        ChatMessage message
) {
    public static ChatMessageCreatedEvent of(Long roomId, ChatMessage message) {
        return new ChatMessageCreatedEvent(roomId, message);
    }
}
