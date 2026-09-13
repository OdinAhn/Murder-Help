package org.example.murderhelp.domain.chat.event;

import org.example.murderhelp.domain.chat.entity.ChatRoom;

public record ChatRoomUpdatedEvent(
        ChatRoom room
) {
    public static ChatRoomUpdatedEvent from(ChatRoom room) {
        return new ChatRoomUpdatedEvent(room);
    }
}
