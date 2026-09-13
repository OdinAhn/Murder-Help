package org.example.murderhelp.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.redis.ChatRedisPublisher;
import org.example.murderhelp.domain.chat.repository.ChatRoomRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.service.MemberService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRedisPublisher chatRedisPublisher;
    private final MemberService memberService;

    @Transactional
    public ChatRoomResponse createRoom(Long memberId) {
        boolean hasActiveRoom = chatRoomRepository.existsByCustomerIdAndStatusIn(
                memberId,
                List.of(ChatRoomStatus.WAITING, ChatRoomStatus.IN_PROGRESS)
        );

        if (hasActiveRoom) {
            throw new BusinessException(ErrorCode.ALREADY_ACTIVE_ROOM_EXISTS);
        }

        Member customer = memberService.getMemberById(memberId);

        String randomHash = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String generatedTitle = String.format("REQ-CUST%03d-%s", memberId, randomHash);

        ChatRoom room = ChatRoom.builder()
                .title(generatedTitle)
                .customer(customer)
                .build();
        
        chatRoomRepository.save(room);
        ChatRoomResponse response = ChatRoomResponse.from(room);
        // Redis Pub/Sub을 통해 관리자 대시보드로 브로드캐스트
        chatRedisPublisher.publishRoomUpdate(response);

        return response;
    }

    public Page<ChatRoomResponse> getRooms(Long customerId, ChatRoomStatus status, Pageable pageable) {
        return chatRoomRepository.findRoomsByCondition(customerId, status, pageable)
                .map(ChatRoomResponse::from);
    }

    public ChatRoomResponse getRoom(Long roomId) {
        return ChatRoomResponse.from(getRoomEntity(roomId));
    }

    // 서비스 간 내부 호출용 엔티티 반환 메서드
    public ChatRoom getRoomEntity(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Transactional
    public void updateLastMessageTime(Long roomId) {
        chatRoomRepository.updateLastMessageTime(roomId);
    }

    @Transactional
    public void closeRoom(Long roomId) {
        ChatRoom room = getRoomEntity(roomId);
        room.closeRoom(); 
        
        ChatRoomResponse response = ChatRoomResponse.from(room);

        // Redis Pub/Sub을 통해 관리자 대시보드로 상태 변경 브로드캐스트
        chatRedisPublisher.publishRoomUpdate(response);
    }
}
