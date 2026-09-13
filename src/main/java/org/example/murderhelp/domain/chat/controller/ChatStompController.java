package org.example.murderhelp.domain.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageSendRequest;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;

    // 실시간 메시지 브로드캐스트 (WebSocket / STOMP)
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload @Valid ChatMessageSendRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        
        Long memberId = (Long) headerAccessor.getSessionAttributes().get("memberId");
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 프론트엔드에서 보낸 memberId를 무시하고, 검증된 세션의 memberId를 강제 삽입
        ChatMessageSendRequest secureRequest = new ChatMessageSendRequest(
                request.roomId(),
                memberId,
                request.content(),
                request.messageType()
        );

        chatMessageService.sendMessage(secureRequest);
    }
}
