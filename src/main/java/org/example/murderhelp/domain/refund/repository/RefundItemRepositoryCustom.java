package org.example.murderhelp.domain.refund.repository;

import org.example.murderhelp.domain.refund.repository.dto.RefundedQuantity;

import java.util.List;

public interface RefundItemRepositoryCustom {
    List<RefundedQuantity> findRefundedQuantitiesByOrderItemIds(List<Long> orderItemIds);
}