package org.example.murderhelp.domain.payment.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.payment.entity.Payment;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.example.murderhelp.domain.order.entity.QOrder;
import org.example.murderhelp.domain.order.entity.QOrderItem;
import org.example.murderhelp.domain.payment.entity.QPayment;
import org.example.murderhelp.domain.payment.repository.dto.PaymentWithItems;

@RequiredArgsConstructor
public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QOrder order = QOrder.order;
    private final QOrderItem orderItem = QOrderItem.orderItem;
    private final QPayment payment = QPayment.payment;


    // 주문 단건 조회 화면 - 결제 ID만
    @Override
    public Optional<Long> findIdByOrderId(Long orderId) {
        Long result = queryFactory
                .select(payment.id)
                .from(payment)
                .where(payment.order.id.eq(orderId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    // 주문 목록 조회 - N+1 방지
    @Override
    public List<Object[]> findIdsByOrderIds(List<Long> orderIds) {
        List<Tuple> tuples = queryFactory
                .select(payment.order.id, payment.id)
                .from(payment)
                .where(payment.order.id.in(orderIds))
                .fetch();

        return tuples.stream()
                .map(t -> new Object[]{t.get(payment.order.id), t.get(payment.id)})
                .collect(Collectors.toList());
    }

    // Webhook에서 받아온 portonePaymentId 조건으로 Payment 조회 시 연관된 Order를 fetch join 으로 함께 로딩
    @Override
    public Optional<Payment> findByPortonePaymentId(String portonePaymentId) {
        Payment result = queryFactory
                .selectFrom(payment)
                .join(payment.order, order).fetchJoin()
                .where(payment.portonePaymentId.eq(portonePaymentId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    // 결제 확정 - orderId 기준 조회 (Order fetch join)
    @Override
    public Optional<Payment> findByOrderIdWithOrder(Long orderId) {
        Payment result = queryFactory
                .selectFrom(payment)
                .join(payment.order, order).fetchJoin()
                .where(payment.order.id.eq(orderId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    // 결제 상세 조회 - paymentId 기준 (Order fetch join)
    @Override
    public Optional<Payment> findByIdWithOrder(Long paymentId) {
        Payment result = queryFactory
                .selectFrom(payment)
                .join(payment.order, order).fetchJoin()
                .where(payment.id.eq(paymentId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    // 결제 상세 조회 - paymentId 기준 (Order, OrderItems fetch join)
    @Override
    public Optional<PaymentWithItems> findByIdWithOrderAndItems(Long paymentId) {
        Payment result = queryFactory
                .selectFrom(payment)
                .join(payment.order, order).fetchJoin()
                .where(payment.id.eq(paymentId))
                .fetchOne();

        if (result == null) {
            return Optional.empty();
        }

        List<OrderItem> items = queryFactory
                .selectFrom(orderItem)
                .where(orderItem.order.eq(result.getOrder()))
                .fetch();

        return Optional.of(new PaymentWithItems(result, items));
    }

    @Override
    public List<Payment> findByOrderIdIn(List<Long> orderIds) {
        return queryFactory
                .selectFrom(payment)
                .join(payment.order, order).fetchJoin()
                .where(payment.order.id.in(orderIds))
                .fetch();
    }

    // 환불 처리 시 결제건(Payment)에만 단일 비관적 락을 획득하여 이중 환불을 방지합니다.
    @Override
    public Optional<Payment> findByIdForRefundLockOnly(Long paymentId) {
        Payment result = queryFactory
                .selectFrom(payment)
                .where(payment.id.eq(paymentId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Payment> findByOrderIdWithOrderForUpdate(Long orderId) {
        Payment result = queryFactory
                .selectFrom(payment)
                .join(payment.order, order).fetchJoin()
                .where(payment.order.id.eq(orderId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();
        return Optional.ofNullable(result);
    }

}




