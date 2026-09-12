package org.example.murderhelp.domain.order.service;

import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.domain.order.dto.CreateOrderRequest;
import org.example.murderhelp.domain.order.dto.CreateOrderResponse;
import org.example.murderhelp.domain.order.dto.OrderListPeriod;
import org.example.murderhelp.domain.order.dto.OrderListRequest;
import org.example.murderhelp.domain.order.dto.OrderResponse;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.order.repository.OrderItemRepository;
import org.example.murderhelp.domain.order.repository.OrderRepository;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private MemberRepository memberRepository;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<List<OrderItem>> orderItemListCaptor;

    private final Pageable pageable = PageRequest.of(0, 10);

    // ─── 주문 목록 조회 ───

    @Test
    void 주문_목록이_없으면_빈_페이지를_반환한다() {
        // given
        OrderListRequest request = new OrderListRequest(OrderListPeriod.MONTH_3, null);
        when(orderRepository.findAllListPage(eq(1L), any(LocalDateTime.class), isNull(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        // when
        Page<OrderResponse> result = orderService.getOrderList(1L, request, pageable);

        // then
        assertThat(result).isEmpty();
        verify(orderItemRepository, never()).findAllWithReviewByOrderIdIn(anyList());
    }

    @Test
    void 조회_기간이_ALL이면_시작일_없이_조회한다() {
        // given
        OrderListRequest request = new OrderListRequest(OrderListPeriod.ALL, OrderStatus.PAID);
        when(orderRepository.findAllListPage(anyLong(), any(), any(), any()))
                .thenReturn(Page.empty(pageable));

        // when
        orderService.getOrderList(1L, request, pageable);

        // then
        verify(orderRepository).findAllListPage(eq(1L), isNull(), eq(OrderStatus.PAID), eq(pageable));
    }

    @Test
    void 주문_목록을_주문별_주문상품과_함께_반환한다() {
        // given
        Member member = createMember(1L);
        Order order1 = createOrderEntity(10L, member);
        Order order2 = createOrderEntity(20L, member);

        Product pistol = createProduct("권총", 100_000L);
        Product knife = createProduct("단검", 30_000L);
        OrderItem orderItem1 = new OrderItem(order1, pistol, 1);
        OrderItem orderItem2 = new OrderItem(order1, knife, 2);

        OrderListRequest request = new OrderListRequest(OrderListPeriod.MONTH_3, null);
        when(orderRepository.findAllListPage(eq(1L), any(LocalDateTime.class), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(order1, order2), pageable, 2));
        when(orderItemRepository.findAllWithReviewByOrderIdIn(List.of(10L, 20L)))
                .thenReturn(List.of(orderItem1, orderItem2));

        // when
        Page<OrderResponse> result = orderService.getOrderList(1L, request, pageable);

        // then
        assertThat(result.getContent())
                .extracting(OrderResponse::orderNumber)
                .containsExactly("ORD-10", "ORD-20");
        assertThat(result.getContent().get(0).items())
                .extracting(OrderResponse.Item::productName)
                .containsExactly("권총", "단검");
        // 주문상품이 없는 주문은 빈 목록으로 내려간다
        assertThat(result.getContent().get(1).items()).isEmpty();
    }

    // ─── 주문 상세 조회 ───

    @Test
    void 본인_주문이면_주문_상세를_반환한다() {
        // given
        Order order = createOrderEntity(10L, createMember(1L));
        OrderItem orderItem = new OrderItem(order, createProduct("권총", 100_000L), 2);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrder_Id(10L)).thenReturn(List.of(orderItem));

        // when
        OrderResponse result = orderService.getOrder(1L, 10L);

        // then
        assertThat(result.orderNumber()).isEqualTo("ORD-10");
        assertThat(result.items())
                .extracting(OrderResponse.Item::productName, OrderResponse.Item::unitPrice, OrderResponse.Item::quantity)
                .containsExactly(tuple("권총", 100_000L, 2));
    }

    @Test
    void 주문이_없으면_예외가_발생한다() {
        // given
        when(orderRepository.findById(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.getOrder(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    void 다른_회원의_주문이면_주문이_없다는_예외가_발생한다() {
        // given
        Order order = createOrderEntity(10L, createMember(2L));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.getOrder(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        verify(orderItemRepository, never()).findAllByOrder_Id(anyLong());
    }

    // ─── 주문 생성 ───

    @Test
    void 장바구니_상품으로_주문과_주문상품을_저장한다() {
        // given
        Member member = createMember(1L);
        Product pistol = createProduct("권총", 100_000L);
        Product knife = createProduct("단검", 30_000L);

        List<CartItemResponse> cartItemList = List.of(
                new CartItemResponse(100L, 1L, 1),
                new CartItemResponse(101L, 2L, 3)
        );
        Map<Long, Product> productMap = Map.of(1L, pistol, 2L, knife);
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(100L, 101L), "홍길동", "010-1234-5678", "서울시 강남구", "문 앞에 놔주세요"
        );

        when(memberRepository.getReferenceById(1L)).thenReturn(member);

        // when
        CreateOrderResponse response = orderService.createOrder(1L, request, cartItemList, productMap);

        // then
        // 100,000원 x 1개 + 30,000원 x 3개
        assertThat(response.totalAmount()).isEqualTo(190_000L);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.orderNumber()).matches("ORD-\\d{14}-[0-9A-F]{8}");

        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getMember()).isSameAs(member);
        assertThat(savedOrder.getReceiverName()).isEqualTo("홍길동");
        assertThat(savedOrder.getDeliveryRequest()).isEqualTo("문 앞에 놔주세요");

        verify(orderItemRepository).saveAll(orderItemListCaptor.capture());
        List<OrderItem> savedOrderItems = orderItemListCaptor.getValue();
        assertThat(savedOrderItems)
                .extracting(OrderItem::getProductName, OrderItem::getUnitPrice, OrderItem::getQuantity)
                .containsExactly(
                        tuple("권총", 100_000L, 1),
                        tuple("단검", 30_000L, 3)
                );
        assertThat(savedOrderItems).allMatch(orderItem -> orderItem.getOrder() == savedOrder);
    }

    // ─── 테스트 데이터 ───

    private Member createMember(Long id) {
        Member member = Member.builder()
                .email("member" + id + "@test.com")
                .password("password")
                .name("회원" + id)
                .phone("010-0000-0000")
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Order createOrderEntity(Long id, Member member) {
        Order order = Order.builder()
                .member(member)
                .orderNumber("ORD-" + id)
                .totalAmount(10_000L)
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .deliveryAddress("서울시 강남구")
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    // Product는 외부에서 쓸 수 있는 생성자가 없어서 mock으로 만든다
    private Product createProduct(String name, long price) {
        Product product = mock(Product.class);
        when(product.getName()).thenReturn(name);
        when(product.getPrice()).thenReturn(price);
        return product;
    }

}
