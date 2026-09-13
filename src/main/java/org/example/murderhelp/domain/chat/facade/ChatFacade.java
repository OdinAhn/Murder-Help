package org.example.murderhelp.domain.chat.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.example.murderhelp.domain.chat.service.ChatRoomService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatFacade {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final org.example.murderhelp.domain.chat.redis.ChatRedisPublisher chatRedisPublisher;

    /**
     * 방 생성 + 챗봇 첫 인사 발송 오케스트레이션
     */
    public ChatRoomResponse createRoomAndSendGreeting(Long memberId) {
        log.info("========== [채팅방 생성 파사드 진입] ==========");

        ChatRoomResponse roomResponse = chatRoomService.createRoom(memberId);

        chatMessageService.sendBotWelcomeMessage(roomResponse.roomId());
        chatRedisPublisher.publishRoomUpdate(roomResponse);

        log.info("채팅방 생성 및 챗봇 환영 메시지 발송 완료. Room ID: {}", roomResponse.roomId());
        
        return roomResponse;
    }

    /**
     * 방 닫기 + 종료 시스템 메시지 발송 오케스트레이션
     */
    public void closeRoom(Long roomId) {
        log.info("========== [채팅방 종료 파사드 진입] ==========");
        
        chatRoomService.closeRoom(roomId);
        chatMessageService.sendCloseSystemMessage(roomId);
        
        log.info("채팅방 종료 및 시스템 메시지 발송 완료. Room ID: {}", roomId);
    }
}
