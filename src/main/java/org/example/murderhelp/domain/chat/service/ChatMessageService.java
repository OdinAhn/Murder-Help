package org.example.murderhelp.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatMessageSendRequest;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.redis.ChatRedisPublisher;
import org.example.murderhelp.domain.chat.repository.ChatMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final ChatRedisPublisher chatRedisPublisher;

    @Transactional
    public void sendMessage(ChatMessageSendRequest request) {

        ChatRoom room = chatRoomService.getRoomEntity(request.roomId());

        room.getStatus().validateMessageSendable();

        // 관리자(고객 본인이 아닌 사람)가 WAITING 상태의 방에 첫 답변을 보낼 때만 IN_PROGRESS로 전환
        boolean isCustomer = room.getCustomerId().equals(request.memberId());
        if (!isCustomer && room.getStatus() == ChatRoomStatus.WAITING && room.getAdminId() == null) {
            room.assignAdmin(request.memberId());
            // Redis Pub/Sub을 통해 관리자 대시보드로 상태 변경 브로드캐스트
            chatRedisPublisher.publishRoomUpdate(ChatRoomResponse.from(room));
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .memberId(request.memberId())
                .content(request.content())
                .build();
        
        chatMessageRepository.save(message);
        chatRoomService.updateLastMessageTime(room.getId());

        // Redis Pub/Sub을 통해 메시지 발행 (다중 서버 브로드캐스트)
        ChatMessageResponse response = ChatMessageResponse.from(message);
        chatRedisPublisher.publish(room.getId(), response);
    }

    public Page<ChatMessageResponse> getMessageHistory(Long roomId, Pageable pageable) {
        return chatMessageRepository.findMessagesByRoomId(roomId, pageable)
                .map(ChatMessageResponse::from);
    }
}
