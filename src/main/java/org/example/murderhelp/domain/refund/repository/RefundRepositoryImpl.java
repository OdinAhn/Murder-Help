package org.example.murderhelp.domain.refund.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.order.entity.QOrderItem;
import org.example.murderhelp.domain.product.entity.QProduct;
import org.example.murderhelp.domain.refund.entity.*;
import org.example.murderhelp.domain.refund.repository.dto.RefundWithItems;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RefundRepositoryImpl implements RefundRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QRefund refund = QRefund.refund;
    private final QRefundItem refundItem = QRefundItem.refundItem;
    private final QOrderItem orderItem = QOrderItem.orderItem;
    private final QProduct product = QProduct.product;

    @Override
    public List<RefundWithItems> findByPaymentIdWithItems(Long paymentId) {
        List<Refund> refunds = queryFactory
                .selectFrom(refund)
                .where(refund.payment.id.eq(paymentId))
                .orderBy(refund.createdAt.desc())
                .fetch();

        if (refunds.isEmpty()) {
            return List.of();
        }

        List<RefundItem> items = queryFactory
                .selectFrom(refundItem)
                .join(refundItem.orderItem, orderItem).fetchJoin()
                .join(orderItem.product, product).fetchJoin()
                .where(refundItem.refund.in(refunds))
                .fetch();

        Map<Long, List<RefundItem>> itemsByRefundId = items.stream()
                .collect(Collectors.groupingBy(ri -> ri.getRefund().getId()));

        return refunds.stream()
                .map(r -> new RefundWithItems(r, itemsByRefundId.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Override
    public Long sumRefundedPgAmountByPaymentId(Long paymentId) {
        Long sum = queryFactory
                .select(refund.pgRefundAmount.sum())
                .from(refund)
                .where(refund.payment.id.eq(paymentId)
                        .and(refund.status.eq(RefundStatus.COMPLETED)))
                .fetchOne();

        return sum != null ? sum : 0;
    }
}