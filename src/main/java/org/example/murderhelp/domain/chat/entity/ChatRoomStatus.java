package org.example.murderhelp.domain.chat.entity;

public enum ChatRoomStatus {
    BOT_MODE {
        @Override
        public boolean canSendMessage(boolean isCustomer) {
            // 봇 모드에서는 고객(Customer)만 메시지를 보낼 수 있음
            return isCustomer;
        }

        @Override
        public boolean isBotActive() {
            return true;
        }
    },
    WAITING,      // 기본 동작 사용
    IN_PROGRESS,  // 기본 동작 사용
    COMPLETED {
        @Override
        public boolean canSendMessage(boolean isCustomer) {
            // 종료된 방에서는 아무도 메시지를 보낼 수 없음
            return false;
        }

        @Override
        public boolean canClose() {
            // 이미 종료된 방은 다시 종료할 수 없음
            return false;
        }
    };

    // --- Default Methods (기본적으로 허용되는 동작들을 정의) ---

    public boolean canSendMessage(boolean isCustomer) {
        return true; // WAITING, IN_PROGRESS의 기본 동작
    }

    public boolean canClose() {
        return true; // BOT_MODE, WAITING, IN_PROGRESS의 기본 동작
    }

    public boolean isBotActive() {
        return false; // BOT_MODE 외의 기본 동작
    }
}
