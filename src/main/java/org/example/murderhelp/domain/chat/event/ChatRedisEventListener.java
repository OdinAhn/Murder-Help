package org.example.murderhelp.domain.chat.event;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.redis.ChatRedisPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatRedisEventListener {

    private final ChatRedisPublisher chatRedisPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageCreatedEvent(ChatMessageCreatedEvent event) {
        chatRedisPublisher.publish(event.roomId(), ChatMessageResponse.from(event.message()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatRoomUpdatedEvent(ChatRoomUpdatedEvent event) {
        chatRedisPublisher.publishRoomUpdate(ChatRoomResponse.from(event.room()));
    }
}
