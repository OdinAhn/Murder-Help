package org.example.murderhelp.domain.chat.entity;

import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;

public enum ChatRoomStatus {
    WAITING {
        @Override
        public void validateMessageSendable() {
            // 전송 가능 (아무 작업도 하지 않음)
        }
    },
    IN_PROGRESS {
        @Override
        public void validateMessageSendable() {
            // 전송 가능 (아무 작업도 하지 않음)
        }
    },
    COMPLETED {
        @Override
        public void validateMessageSendable() {
            // 종료된 방이므로 예외 발생
            throw new BusinessException(ErrorCode.INVALID_CHAT_ROOM_STATUS);
        }
    };

    public abstract void validateMessageSendable();
}
