package org.example.murderhelp.domain.payment.repository;

import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.repository.dto.PaymentWithItems;

import java.util.List;
import java.util.Optional;

public interface  PaymentRepositoryCustom {

    Optional<Long> findIdByOrderId(Long orderId);

    List<Object[]> findIdsByOrderIds(List<Long> orderIds);

    Optional<Payment> findByPortonePaymentId(String portonePaymentId);

    Optional<Payment> findByOrderIdWithOrder(Long orderId);

    Optional<Payment> findByIdWithOrder(Long paymentId);

    Optional<PaymentWithItems> findByIdWithOrderAndItems(Long paymentId);

    List<Payment> findByOrderIdIn(List<Long> orderIds);

    Optional<Payment> findByIdForRefundLockOnly(Long paymentId);

    Optional<Payment> findByOrderIdWithOrderForUpdate(Long orderId);

}