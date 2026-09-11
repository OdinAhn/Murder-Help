package org.example.murderhelp.domain.chat.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber implements MessageListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // RedisConfig에 등록해둔 ValueSerializer를 사용해 역직렬화!
            Object deserialized = redisTemplate.getValueSerializer().deserialize(message.getBody());
            
            if (deserialized instanceof ChatMessageResponse response) {
                // 로컬 웹소켓 클라이언트들에게 브로드캐스트
                messagingTemplate.convertAndSend("/sub/chat/room/" + response.roomId(), response);
            } else if (deserialized instanceof ChatRoomResponse roomResponse) {
                // 관리자 대시보드 웹소켓 구독자들에게 브로드캐스트
                messagingTemplate.convertAndSend("/sub/chat/rooms/updates", roomResponse);
            }
        } catch (Exception e) {
            log.error("Redis 메시지 역직렬화 실패", e);
        }
    }
}
