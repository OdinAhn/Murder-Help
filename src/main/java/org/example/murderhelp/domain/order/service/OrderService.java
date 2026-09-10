package org.example.murderhelp.domain.order.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.order.dto.OrderListRequest;
import org.example.murderhelp.domain.order.dto.OrderResponse;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.repository.OrderItemRepository;
import org.example.murderhelp.domain.order.repository.OrderRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Page<OrderResponse> getOrderList(OrderListRequest orderListRequest, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAllListPage(
                orderListRequest.period().toLocalDateTime(),
                orderListRequest.status(),
                pageable
        );

        if (orderPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> orderIds = orderPage.getContent().stream()
                .map(Order::getId)
                .toList();

        Map<Long, List<OrderItem>> orderItemsMap =
                orderItemRepository.findAllWithReviewByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(orderItem -> orderItem.getOrder().getId()));

        return orderPage.map(order -> {
            List<OrderResponse.Item> items = orderItemsMap
                    .getOrDefault(order.getId(), List.of())
                    .stream()
                    .map(OrderResponse.Item::from)
                    .toList();

            return OrderResponse.from(order, items);
        });
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderItem> orderItemList = orderItemRepository.findAllByOrder_Id(orderId);

        return OrderResponse.from(order, orderItemList.stream().map(OrderResponse.Item::from).toList());
    }

}
