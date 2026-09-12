package org.example.murderhelp.domain.payment.entity;

public enum PaymentStatus {

    /**
     * 결제 상태 머신
     * - PENDING → COMPLETED       : 결제 성공 (PortOne 승인 + 서버 3대 조건 검증 통과)
     * - PENDING → FAILED          : 결제 미완료 (PG 거절, 서버 검증 실패 등)
     * - PENDING → CANCELLED       : 결제 대기 중 사용자가 직접 취소 (결제창 이탈 등, PG 미승인 상태)
     * - COMPLETED → PARTIAL_REFUND: 완료된 결제 중 일부 금액 환불
     * - COMPLETED → FULL_REFUND   : 완료된 결제 전액 환불
     * - PARTIAL_REFUND → FULL_REFUND: 부분환불 후 잔여 전액 환불
     *
     * - FAILED        = 결제 완료 전 종결되는 모든 케이스 (PG거절/서버검증실패, 세부 원인은 fail_reason 컬럼)
     * - CANCELLED     = 결제 완료 전 사용자가 직접 취소한 경우 (PG 승인 자체가 없어 보상 취소 불필요)
     * - PARTIAL_REFUND/FULL_REFUND = 결제 완료 후에만 도달 가능한 상태
     */
    
    PENDING {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == COMPLETED || target == FAILED || target == CANCELLED;
        }
    },
    COMPLETED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == PARTIAL_REFUND || target == FULL_REFUND;
        }
    },
    FAILED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }
    },
    CANCELLED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }
    },
    PARTIAL_REFUND {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == PARTIAL_REFUND || target == FULL_REFUND;
        }
    },
    FULL_REFUND {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }
    };

    public abstract boolean canTransitTo(PaymentStatus target);
}
