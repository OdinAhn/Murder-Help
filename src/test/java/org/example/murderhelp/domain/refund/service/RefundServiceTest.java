package org.example.murderhelp.domain.refund.service;

import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.payment.entity.Payment;
import org.example.murderhelp.domain.payment.repository.dto.PaymentWithItems;
import org.example.murderhelp.domain.payment.service.PaymentService;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.repository.ProductRepository;
import org.example.murderhelp.domain.refund.component.RefundCalculator;
import org.example.murderhelp.domain.refund.component.RefundCalculator.RefundCalculationResult;
import org.example.murderhelp.domain.refund.dto.RefundRequest;
import org.example.murderhelp.domain.refund.dto.RefundRequest.RefundItemRequest;
import org.example.murderhelp.domain.refund.dto.RefundableItemResponse;
import org.example.murderhelp.domain.refund.entity.Refund;
import org.example.murderhelp.domain.refund.entity.RefundItem;
import org.example.murderhelp.domain.refund.entity.RefundStatus;
import org.example.murderhelp.domain.refund.repository.RefundItemRepository;
import org.example.murderhelp.domain.refund.repository.RefundRepository;
import org.example.murderhelp.domain.refund.repository.dto.RefundWithItems;
import org.example.murderhelp.domain.refund.repository.dto.RefundedQuantity;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RefundService.calculateAndSaveRefund() 의 검증 로직과, 과거 실제로 있었던
 * "RefundItem 이 연관관계만 세팅되고 저장되지 않던 버그"에 대한 회귀 테스트를 포함한다.
 */
@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private RefundCalculator refundCalculator;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private RefundItemRepository refundItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private RefundService refundService;

    private static final Long MEMBER_ID = 1L;
    private static final Long PAYMENT_ID = 200L;
    private static final Long ORDER_ITEM_ID = 10L;
    private static final Long PRODUCT_ID = 50L;

    private Member member;
    private Order order;
    private Payment payment;
    private Product product;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(MEMBER_ID);

        order = mock(Order.class);
        lenient().when(order.getMember()).thenReturn(member);

        payment = mock(Payment.class);
        lenient().when(payment.getId()).thenReturn(PAYMENT_ID);
        lenient().when(payment.getOrder()).thenReturn(order);
        lenient().when(payment.getPgAmount()).thenReturn(27_000L);

        product = mock(Product.class);
        lenient().when(product.getId()).thenReturn(PRODUCT_ID);

        orderItem = mock(OrderItem.class);
        lenient().when(orderItem.getId()).thenReturn(ORDER_ITEM_ID);
        lenient().when(orderItem.getQuantity()).thenReturn(2);
        lenient().when(orderItem.getProduct()).thenReturn(product);
    }

    private RefundRequest requestFor(int quantity) {
        return new RefundRequest(PAYMENT_ID, "단순 변심", List.of(new RefundItemRequest(ORDER_ITEM_ID, quantity)));
    }

    private void stubBaseFlow(List<RefundWithItems> existingRefunds, RefundCalculationResult calcResult) {
        PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of(orderItem));
        when(paymentService.findForRefund(PAYMENT_ID)).thenReturn(paymentWithItems);
        when(refundRepository.findByPaymentIdWithItems(PAYMENT_ID)).thenReturn(existingRefunds);
        lenient().when(refundCalculator.calculate(any(), anyMap(), eq(payment), anyBoolean(), anyLong()))
                .thenReturn(calcResult);
    }

    @Nested
    @DisplayName("calculateAndSaveRefund - 검증")
    class Validation {

        @Test
        @DisplayName("5초 이내 동일 결제건 환불 이력이 있으면 중복 요청 예외를 던진다")
        void rejectsDuplicateRequestWithinFiveSeconds() {
            Refund recentRefund = mock(Refund.class);
            when(recentRefund.getCreatedAt()).thenReturn(LocalDateTime.now().minusSeconds(2));
            RefundWithItems duplicate = new RefundWithItems(recentRefund, List.of());

            stubBaseFlow(List.of(duplicate), null);

            assertThatThrownBy(() -> refundService.calculateAndSaveRefund(MEMBER_ID, requestFor(1)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_REFUND_REQUEST);

            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("5초가 지난 이전 환불 이력은 중복으로 보지 않는다")
        void doesNotTreatOldRefundAsDuplicate() {
            Refund oldRefund = mock(Refund.class);
            when(oldRefund.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(10));
            when(oldRefund.getStatus()).thenReturn(RefundStatus.COMPLETED);
            when(oldRefund.getPgRefundAmount()).thenReturn(9_000L);
            RefundWithItems oldOne = new RefundWithItems(oldRefund, List.of());

            RefundItem newRefundItem = mock(RefundItem.class);
            RefundCalculationResult calcResult = new RefundCalculationResult(List.of(newRefundItem), 9_000L);
            stubBaseFlow(List.of(oldOne), calcResult);

            when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

            // 남은 수량이 1개(전체 2개 중 1개는 이전 이력에 이미 반영됐다고 가정하지 않고,
            // 여기서는 요청 수량이 남은 수량 이내인 정상 케이스만 확인
            refundService.calculateAndSaveRefund(MEMBER_ID, requestFor(1));

            verify(refundRepository).save(any(Refund.class));
        }

        @Test
        @DisplayName("본인 소유가 아닌 결제 건이면 접근 거부 예외를 던진다")
        void rejectsWhenNotOwner() {
            stubBaseFlow(List.of(), null);

            assertThatThrownBy(() -> refundService.calculateAndSaveRefund(999L, requestFor(1)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REFUND_ACCESS_DENIED);
        }

        @Test
        @DisplayName("존재하지 않는 주문상품 ID로 요청하면 예외를 던진다")
        void rejectsUnknownOrderItemId() {
            stubBaseFlow(List.of(), null);

            RefundRequest request = new RefundRequest(
                    PAYMENT_ID, "단순 변심", List.of(new RefundItemRequest(9999L, 1)));

            assertThatThrownBy(() -> refundService.calculateAndSaveRefund(MEMBER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REFUND_ITEM_NOT_FOUND);
        }

        @Test
        @DisplayName("남은 수량보다 많이 환불 요청하면 예외를 던진다")
        void rejectsWhenExceedingRemainingQuantity() {
            // orderItem 의 원래 수량은 2개, 3개를 요청하면 초과
            stubBaseFlow(List.of(), null);

            assertThatThrownBy(() -> refundService.calculateAndSaveRefund(MEMBER_ID, requestFor(3)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EXCEED_REFUNDABLE_QUANTITY);

            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 환불된 수량을 제외한 남은 수량만큼만 요청 가능하다")
        void accountsForAlreadyRefundedQuantity() {
            RefundItem previouslyRefundedItem = mock(RefundItem.class);
            when(previouslyRefundedItem.getOrderItem()).thenReturn(orderItem);
            when(previouslyRefundedItem.getRefundQuantity()).thenReturn(1);

            Refund completedRefund = mock(Refund.class);
            when(completedRefund.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(10));
            when(completedRefund.getStatus()).thenReturn(RefundStatus.COMPLETED);
            RefundWithItems existing = new RefundWithItems(completedRefund, List.of(previouslyRefundedItem));

            stubBaseFlow(List.of(existing), null);

            // 원래 수량 2개 중 1개는 이미 환불됨 -> 남은 건 1개인데 2개를 요청하면 초과
            assertThatThrownBy(() -> refundService.calculateAndSaveRefund(MEMBER_ID, requestFor(2)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EXCEED_REFUNDABLE_QUANTITY);
        }
    }

    @Nested
    @DisplayName("calculateAndSaveRefund - 저장 (회귀 테스트)")
    class Persistence {

        @Test
        @DisplayName("[회귀] 계산된 RefundItem 은 반드시 refundItemRepository 로 저장되어야 한다")
        void savesRefundItemsExplicitly() {
            RefundItem calculatedItem = mock(RefundItem.class);
            RefundCalculationResult calcResult = new RefundCalculationResult(List.of(calculatedItem), 9_000L);
            stubBaseFlow(List.of(), calcResult);

            Refund savedRefund = mock(Refund.class);
            when(refundRepository.save(any(Refund.class))).thenReturn(savedRefund);

            refundService.calculateAndSaveRefund(MEMBER_ID, requestFor(1));

            // 연관관계 세팅만 하고 저장을 빼먹는 버그가 재발하면 이 검증에서 바로 잡힌다
            verify(calculatedItem).assignRefund(savedRefund);
            verify(refundItemRepository).saveAll(calcResult.refundItems());
        }

        @Test
        @DisplayName("요청 수량이 남은 수량 전부와 같으면 전액 환불로 처리한다")
        void treatsAsFullRefundWhenRequestingAllRemainingQuantity() {
            RefundItem calculatedItem = mock(RefundItem.class);
            RefundCalculationResult calcResult = new RefundCalculationResult(List.of(calculatedItem), 27_000L);
            stubBaseFlow(List.of(), calcResult);
            when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

            // orderItem 수량 전체(2개)를 요청 -> 전액/마지막 환불
            refundService.calculateAndSaveRefund(MEMBER_ID, requestFor(2));

            verify(payment).fullRefund();
            verify(order).transitTo(OrderStatus.CANCELED);
            verify(payment, never()).partialRefund();

            ArgumentCaptor<Boolean> isFullRefundCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(refundCalculator).calculate(any(), anyMap(), eq(payment), isFullRefundCaptor.capture(), anyLong());
            assertThat(isFullRefundCaptor.getValue()).isTrue();
        }

        @Test
        @DisplayName("요청 수량이 남은 수량보다 적으면 부분 환불로 처리하고 주문은 취소하지 않는다")
        void treatsAsPartialRefundWhenRequestingLessThanRemaining() {
            RefundItem calculatedItem = mock(RefundItem.class);
            RefundCalculationResult calcResult = new RefundCalculationResult(List.of(calculatedItem), 9_000L);
            stubBaseFlow(List.of(), calcResult);
            when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

            // orderItem 수량 2개 중 1개만 요청 -> 부분 환불
            refundService.calculateAndSaveRefund(MEMBER_ID, requestFor(1));

            verify(payment).partialRefund();
            verify(payment, never()).fullRefund();
            verify(order, never()).transitTo(any());

            ArgumentCaptor<Boolean> isFullRefundCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(refundCalculator).calculate(any(), anyMap(), eq(payment), isFullRefundCaptor.capture(), anyLong());
            assertThat(isFullRefundCaptor.getValue()).isFalse();
        }

        @Test
        @DisplayName("환불 대상 상품의 재고를 요청 수량만큼 복구한다")
        void restoresStockForRefundedQuantity() {
            RefundItem calculatedItem = mock(RefundItem.class);
            RefundCalculationResult calcResult = new RefundCalculationResult(List.of(calculatedItem), 9_000L);
            stubBaseFlow(List.of(), calcResult);
            when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

            refundService.calculateAndSaveRefund(MEMBER_ID, requestFor(1));

            verify(product).restoreStock(1);
        }
    }

    @Nested
    @DisplayName("getRefundableItems")
    class GetRefundableItems {

        @Test
        @DisplayName("이미 환불된 수량을 제외한 남은 수량을 반환한다")
        void returnsRemainingQuantityExcludingRefunded() {
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of(orderItem));
            when(paymentService.findByIdWithOrderAndItems(PAYMENT_ID)).thenReturn(paymentWithItems);
            when(refundItemRepository.findRefundedQuantitiesByOrderItemIds(List.of(ORDER_ITEM_ID)))
                    .thenReturn(List.of(new RefundedQuantity(ORDER_ITEM_ID, 1)));

            List<RefundableItemResponse> result = refundService.getRefundableItems(MEMBER_ID, PAYMENT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).originalQuantity()).isEqualTo(2);
            assertThat(result.get(0).remainQuantity()).isEqualTo(1); // 2개 중 1개 이미 환불
        }

        @Test
        @DisplayName("환불 이력이 없으면 원래 수량 그대로 환불 가능하다")
        void returnsOriginalQuantityWhenNoRefundHistory() {
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of(orderItem));
            when(paymentService.findByIdWithOrderAndItems(PAYMENT_ID)).thenReturn(paymentWithItems);
            when(refundItemRepository.findRefundedQuantitiesByOrderItemIds(List.of(ORDER_ITEM_ID)))
                    .thenReturn(List.of());

            List<RefundableItemResponse> result = refundService.getRefundableItems(MEMBER_ID, PAYMENT_ID);

            assertThat(result.get(0).remainQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("본인 소유가 아닌 결제 건이면 접근 거부 예외를 던진다")
        void rejectsWhenNotOwner() {
            PaymentWithItems paymentWithItems = new PaymentWithItems(payment, List.of(orderItem));
            when(paymentService.findByIdWithOrderAndItems(PAYMENT_ID)).thenReturn(paymentWithItems);

            assertThatThrownBy(() -> refundService.getRefundableItems(999L, PAYMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REFUND_ACCESS_DENIED);
        }
    }
}
