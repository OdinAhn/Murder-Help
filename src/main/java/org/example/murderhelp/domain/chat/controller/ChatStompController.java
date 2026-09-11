package org.example.murderhelp.domain.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.chat.dto.ChatMessageSendRequest;
import org.example.murderhelp.domain.chat.service.ChatMessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;

    // 실시간 메시지 브로드캐스트 (WebSocket / STOMP)
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload @Valid ChatMessageSendRequest request) {
        chatMessageService.sendMessage(request);
    }
}
