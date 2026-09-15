package org.example.murderhelp.domain.chat.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.bot.BotScenario;
import org.example.murderhelp.domain.chat.bot.dto.BotMessageDto;
import org.example.murderhelp.domain.chat.dto.ChatMessageResponse;
import org.example.murderhelp.domain.chat.dto.ChatMessageSendRequest;
import org.example.murderhelp.domain.chat.entity.ChatMessage;
import org.example.murderhelp.domain.chat.entity.ChatRoom;
import org.example.murderhelp.domain.chat.entity.ChatRoomStatus;
import org.example.murderhelp.domain.chat.entity.ChatMessageType;
import org.example.murderhelp.domain.chat.repository.ChatMessageRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.service.MemberService;
import org.springframework.context.ApplicationEventPublisher;
import org.example.murderhelp.domain.chat.event.ChatMessageCreatedEvent;
import org.example.murderhelp.domain.chat.event.ChatRoomUpdatedEvent;
import org.example.murderhelp.domain.chat.bot.BotCommandDispatcher;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatRoomService chatRoomService;
    private final MemberService memberService;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final BotCommandDispatcher botCommandDispatcher;
    private final StringRedisTemplate redisTemplate;


    @Transactional
    public void sendBotWelcomeMessage(Long roomId) {
        ChatRoom room = chatRoomService.getRoomEntity(roomId);
        sendBotScenarioMessage(room, BotScenario.WELCOME);
    }

    @Transactional
    public void sendCloseSystemMessage(Long roomId) {
        ChatRoom room = chatRoomService.getRoomEntity(roomId);
        
        ChatMessage closeMsg = ChatMessage.builder()
                .chatRoom(room)
                .sender(memberService.getSystemBotMember())
                .content("[CLOSED] 상담이 완전히 종료되었습니다.")
                .messageType(ChatMessageType.SYSTEM)
                .build();

        // 종료 시스템 메시지 저장/발행
        chatMessageRepository.save(closeMsg);
        updateLastMessageInRedis(room.getId(), closeMsg.getContent());
        publishMessageEvent(room.getId(), closeMsg);
        publishRoomUpdate(room);
    }


    @Transactional
    public void sendMessage(ChatMessageSendRequest request) {
        ChatRoom room = chatRoomService.getRoomEntity(request.roomId());
        Member sender = memberService.getMemberById(request.memberId());
        boolean isCustomer = room.isCustomer(sender.getId());
        
        if (!room.getStatus().canSendMessage(isCustomer)) {
            throw new BusinessException(ErrorCode.INVALID_CHAT_ROOM_STATUS);
        }

        // 관리자 배정
        if (!isCustomer && room.getStatus() == ChatRoomStatus.WAITING && room.getAdmin() == null) {
            room.assignAdmin(sender);
        }

        // 일반 사용자(고객) 메시지
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.content())
                .messageType(request.messageType() != null ? request.messageType() : ChatMessageType.TEXT)
                .build();

        chatMessageRepository.save(message);
        updateLastMessageInRedis(room.getId(), message.getContent());
        publishMessageEvent(room.getId(), message);
        publishRoomUpdate(room);

        // 챗봇 메시지
        if (room.getStatus().isBotActive() && isCustomer) {
            botCommandDispatcher.execute(room, request.content(), this);
        }
        chatRoomService.updateLastMessageTime(room.getId());
    }


    @Transactional
    public void sendSystemMessage(ChatRoom room, String content) {
        ChatMessage sysMsg = ChatMessage.builder()
                .chatRoom(room)
                .sender(memberService.getSystemBotMember())
                .content(content)
                .messageType(ChatMessageType.SYSTEM)
                .build();
        chatMessageRepository.save(sysMsg);
        updateLastMessageInRedis(room.getId(), sysMsg.getContent());
        publishMessageEvent(room.getId(), sysMsg);
        publishRoomUpdate(room);
    }

    @Transactional
    public void sendBotScenarioMessage(ChatRoom room, BotScenario scenario) {
        sendBotMessage(room, scenario.getMessageDto());
    }

    @Transactional
    public void sendBotMessage(ChatRoom room, BotMessageDto messageDto) {
        try {
            String json = objectMapper.writeValueAsString(messageDto);
            ChatMessage botMsg = ChatMessage.builder()
                    .chatRoom(room)
                    .sender(memberService.getSystemBotMember())
                    .content(json)
                    .messageType(ChatMessageType.BUTTON)
                    .build();

            chatMessageRepository.save(botMsg);
            updateLastMessageInRedis(room.getId(), "[챗봇 메시지]");
            publishMessageEvent(room.getId(), botMsg);
            publishRoomUpdate(room);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateLastMessageInRedis(Long roomId, String content) {
        redisTemplate.opsForHash().put("chat_last_messages", roomId.toString(), content);
    }

    private void publishMessageEvent(Long roomId, ChatMessage message) {
        eventPublisher.publishEvent(ChatMessageCreatedEvent.of(roomId, message));
    }

    public void publishRoomUpdate(ChatRoom room) {
        eventPublisher.publishEvent(ChatRoomUpdatedEvent.from(room));
    }

    public Page<ChatMessageResponse> getMessageHistory(Long roomId, Pageable pageable) {
        return chatMessageRepository.findMessagesByRoomId(roomId, pageable)
                .map(ChatMessageResponse::from);
    }
}
