package org.example.murderhelp.domain.chat.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;

@Component
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    // 특정 방(Topic)으로 메시지 발행
    public void publish(Long roomId, ChatMessageResponse message) {
        String topic = "chat-room:" + roomId;
        redisTemplate.convertAndSend(topic, message);
    }

    // 관리자 토픽으로 채팅방 상태/생성 이벤트 발행
    public void publishRoomUpdate(org.example.murderhelp.domain.chat.dto.ChatRoomResponse response) {
        String topic = "chat-room:updates";
        redisTemplate.convertAndSend(topic, response);
    }
}
