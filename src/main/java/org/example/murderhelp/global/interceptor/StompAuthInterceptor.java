package org.example.murderhelp.global.interceptor;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.global.jwt.JwtProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtProvider.validate(token) && !jwtProvider.isRefreshToken(token)) {
                    Long memberId = jwtProvider.getMemberId(token);
                    accessor.getSessionAttributes().put("memberId", memberId);
                } else {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED);
                }
            } else {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
        }
        
        return message;
    }
}
