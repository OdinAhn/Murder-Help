package org.example.murderhelp.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.payment.entity.FailReason;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.repository.PaymentRepository;
import org.example.murderhelp.domain.payment.repository.dto.PaymentWithItems;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // 결제 생성 — pgAmount는 Payment 생성자가 스스로 계산
    @Transactional
    public Payment createPayment(Order order, int amount) {
        Payment payment = Payment.builder()
                .order(order)
                .amount(amount)
                .build();
        return paymentRepository.save(payment);
    }

    public Payment findByOrderIdWithOrder(Long orderId) {
        return paymentRepository.findByOrderIdWithOrder(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    public Payment findByOrderId(Long orderId) {
        return findByOrderIdWithOrder(orderId);
    }

    public Payment findByIdWithOrder(Long paymentId) {
        return paymentRepository.findByIdWithOrder(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    public PaymentWithItems findByIdWithOrderAndItems(Long paymentId) {
        return paymentRepository.findByIdWithOrderAndItems(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    public PaymentWithItems findForRefund(Long paymentId) {

        // 1. Payment 단독 락 획득 (이중 환불 차단)
        paymentRepository.findByIdForRefundLockOnly(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. 락 없이 순수하게 연관 데이터 통째로 조회 (N+1 방지)
        return paymentRepository.findByIdWithOrderAndItems(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    public Payment findByPortonePaymentId(String portonePaymentId) {
        return paymentRepository.findByPortonePaymentId(portonePaymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    // 결제 완료 처리
    @Transactional
    public void completePayment(Payment payment) {

        payment.complete();
    }

    // 결제 실패 처리 (상세 사유 지정)
    @Transactional
    public void failPayment(Payment payment, FailReason reason) {
        payment.fail(reason);
    }

    // 결제 상태 변경(Canceled)
    @Transactional
    public void cancelPayment(Payment payment) {
        payment.cancel();
    }

    public Map<Long, Payment> findPaymentMapByOrderIds(List<Long> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        return paymentRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(p -> p.getOrder().getId(), p -> p));
    }

    public Payment findByOrderIdWithOrderForUpdate(Long orderId) {
        return paymentRepository.findByOrderIdWithOrderForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

}