package org.example.murderhelp.domain.order.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.domain.order.dto.CreateOrderRequest;
import org.example.murderhelp.domain.order.dto.OrderListRequest;
import org.example.murderhelp.domain.order.dto.OrderResponse;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.repository.OrderItemRepository;
import org.example.murderhelp.domain.order.repository.OrderRepository;
import org.example.murderhelp.domain.payment.repository.PaymentRepository;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.review.entity.Review;
import org.example.murderhelp.domain.review.repository.ReviewRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentRepository paymentRepository;

    public Page<OrderResponse> getOrderList(Long memberId, OrderListRequest orderListRequest, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAllListPage(
                memberId,
                orderListRequest.period().toLocalDateTime(),
                orderListRequest.status(),
                pageable
        );

        if (orderPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> orderIds = orderPage.getContent().stream().map(Order::getId).toList();

        List<OrderItem> orderItemList = orderItemRepository.findAllByOrderIdIn(orderIds);
        List<Long> orderItemIds = orderItemList.stream().map(OrderItem::getId).toList();

        Map<Long, List<OrderItem>> orderItemsMap = orderItemList.stream()
                .collect(Collectors.groupingBy(orderItem -> orderItem.getOrder().getId()));

        Map<Long, Review> reviewMap = reviewRepository.findAllByOrderItemIdIn(orderItemIds).stream()
                .collect(Collectors.toMap(Review::getOrderItemId, r -> r));

        // orderId -> paymentId 매핑 (N+1 방지, 기존에 만들어둔 findIdsByOrderIds 재사용)
        Map<Long, Long> paymentIdByOrderId = paymentRepository.findIdsByOrderIds(orderIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return orderPage.map(order -> {
            List<OrderResponse.Item> items = orderItemsMap.getOrDefault(order.getId(), List.of()).stream()
                    .map(orderItem -> OrderResponse.Item.from(orderItem, reviewMap.get(orderItem.getId())))
                    .toList();

            return OrderResponse.from(order, paymentIdByOrderId.get(order.getId()), items);
        });
    }

    public OrderResponse getOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!Objects.equals(order.getMember().getId(), memberId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        List<OrderItem> orderItemList = orderItemRepository.findAllByOrder_Id(orderId);
        Long paymentId = paymentRepository.findIdByOrderId(orderId).orElse(null);

        return OrderResponse.from(order, paymentId, orderItemList.stream().map(OrderResponse.Item::from).toList());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Order createOrder(
            Long memberId,
            CreateOrderRequest createOrderRequest,
            List<CartItemResponse> cartItemList,
            Map<Long, Product> productMap
    ) {
        // 총액 계산
        long totalAmount = 0L;
        for (CartItemResponse cartItem : cartItemList) {
            Product product = productMap.get(cartItem.productId());
            totalAmount += product.getPrice() * cartItem.quantity();
        }

        Order order = Order.create(memberRepository.getReferenceById(memberId), totalAmount, createOrderRequest);

        List<OrderItem> orderItemList = cartItemList.stream()
                .map(cartItem -> new OrderItem(
                        order,
                        productMap.get(cartItem.productId()),
                        cartItem.quantity())
                )
                .toList();

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItemList);
        return order;
    }

}
