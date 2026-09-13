package org.example.murderhelp.domain.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            // 결제 성공 → COMPLETED / 결제 실패·회원 취소 → CANCELED
            return target == PAID || target == CANCELED;
        }
    },

    PAID {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return target == PREPARING_DELIVERY || target == CANCELED;
        }
    },

    PREPARING_DELIVERY {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return target == SHIPPING || target == CANCELED;
        }
    },

    SHIPPING {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return target == DELIVERED || target == CANCELED;
        }
    },

    DELIVERED {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return target == CANCELED;
        }
    },

    CANCELED {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return false;
        }
    };

    public abstract boolean canTransitTo(OrderStatus target);
}
