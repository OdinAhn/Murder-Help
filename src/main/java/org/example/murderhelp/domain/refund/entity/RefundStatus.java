package org.example.murderhelp.domain.refund.entity;

import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;

public enum RefundStatus {
    COMPLETED {
        @Override
        public void validateTransitionTo(RefundStatus newStatus) {
            if (newStatus == PG_FAILED) {
                return;
            }
            throw new BusinessException(ErrorCode.INVALID_REFUND_STATUS);
        }
    },
    PG_FAILED {
        @Override
        public void validateTransitionTo(RefundStatus newStatus) {
            // 실패 상태에서는 어떤 상태로든 변경 불가능
            throw new BusinessException(ErrorCode.INVALID_REFUND_STATUS);
        }
    };

    public abstract void validateTransitionTo(RefundStatus newStatus);
}
