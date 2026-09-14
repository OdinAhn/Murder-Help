package org.example.murderhelp.domain.chat.event;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.redis.ChatRedisPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.springframework.data.redis.core.StringRedisTemplate;

@Component
@RequiredArgsConstructor
public class ChatRedisEventListener {

    private final ChatRedisPublisher chatRedisPublisher;
    private final StringRedisTemplate redisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageCreatedEvent(ChatMessageCreatedEvent event) {
        chatRedisPublisher.publish(event.roomId(), ChatMessageResponse.from(event.message()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatRoomUpdatedEvent(ChatRoomUpdatedEvent event) {
        Object rawMsg = redisTemplate.opsForHash().get("chat_last_messages", event.room().getId().toString());
        String lastMsg = rawMsg != null ? rawMsg.toString() : null;
        chatRedisPublisher.publishRoomUpdate(ChatRoomResponse.from(event.room(), lastMsg));
    }
}
