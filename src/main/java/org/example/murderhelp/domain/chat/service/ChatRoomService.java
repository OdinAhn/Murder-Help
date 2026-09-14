package org.example.murderhelp.domain.chat.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatRoomResponse;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.repository.ChatRoomRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.service.MemberService;
import org.springframework.context.ApplicationEventPublisher;
import org.example.murderhelp.domain.chat.event.ChatRoomUpdatedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final MemberService memberService;
    private final ChatRoomRepository chatRoomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

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
        eventPublisher.publishEvent(ChatRoomUpdatedEvent.from(room));

        return ChatRoomResponse.from(room, null);
    }

    @Transactional
    public void closeRoom(Long roomId) {
        ChatRoom room = getRoomEntity(roomId);
        room.closeRoom();

        eventPublisher.publishEvent(ChatRoomUpdatedEvent.from(room));
    }

 
    public Page<ChatRoomResponse> getRooms(Long customerId, ChatRoomStatus status, String keyword, Pageable pageable) {
        Page<ChatRoom> roomPage = chatRoomRepository.findRoomsByCondition(customerId, status, keyword, pageable);
        List<String> roomIds = roomPage.stream()
                                       .map(room -> room.getId().toString())
                                       .collect(Collectors.toList());

        List<Object> lastMessages = List.of();
        if (!roomIds.isEmpty()) {
            lastMessages = redisTemplate.opsForHash().multiGet("chat_last_messages", (List<Object>)(List<?>) roomIds);
        }

        List<Object> finalLastMessages = lastMessages;
        return roomPage.map(room -> {
            int index = roomIds.indexOf(room.getId().toString());
            String lastMsg = (finalLastMessages != null && index >= 0 && index < finalLastMessages.size()) 
                    ? (String) finalLastMessages.get(index) 
                    : null;
            return ChatRoomResponse.from(room, lastMsg);
        });
    }


    public ChatRoomResponse getRoom(Long roomId) {
        return ChatRoomResponse.from(getRoomEntity(roomId));
    }

    public ChatRoom getRoomEntity(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Transactional
    public void updateLastMessageTime(Long roomId) {
        chatRoomRepository.updateLastMessageTime(roomId);
    }

}
