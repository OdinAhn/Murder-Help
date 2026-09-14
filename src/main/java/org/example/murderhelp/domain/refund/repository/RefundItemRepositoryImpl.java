package org.example.murderhelp.domain.refund.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.refund.entity.QRefundItem;
import org.example.murderhelp.domain.refund.entity.RefundStatus;
import org.example.murderhelp.domain.refund.repository.dto.RefundedQuantity;

import java.util.List;

@RequiredArgsConstructor
public class RefundItemRepositoryImpl implements RefundItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QRefundItem refundItem = QRefundItem.refundItem;

    @Override
    public List<RefundedQuantity> findRefundedQuantitiesByOrderItemIds(List<Long> orderItemIds) {
        return queryFactory
                .select(Projections.constructor(RefundedQuantity.class,
                        refundItem.orderItem.id,
                        refundItem.refundQuantity.sum()))
                .from(refundItem)
                .where(refundItem.orderItem.id.in(orderItemIds)
                        .and(refundItem.refund.status.in(RefundStatus.COMPLETED, RefundStatus.PG_FAILED)))
                .groupBy(refundItem.orderItem.id)
                .fetch();
    }
}
