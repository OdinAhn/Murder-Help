package org.example.murderhelp.domain.refund.repository;

import org.example.murderhelp.domain.refund.repository.dto.RefundWithItems;

import java.util.List;

public interface RefundRepositoryCustom {
    List<RefundWithItems> findByPaymentIdWithItems(Long paymentId);
    Long sumRefundedPgAmountByPaymentId(Long paymentId);
}