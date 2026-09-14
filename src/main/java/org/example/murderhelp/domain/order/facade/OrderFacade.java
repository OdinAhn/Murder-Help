package org.example.murderhelp.domain.order.facade;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.cart.service.CartService;
import org.example.murderhelp.domain.order.dto.CreateOrderRequest;
import org.example.murderhelp.domain.order.dto.CreateOrderResponse;
import org.example.murderhelp.domain.order.entity.Order;
import org.example.murderhelp.domain.order.service.OrderService;
import org.example.murderhelp.domain.payment.service.PaymentService;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.service.ProductCacheEvictionService;
import org.example.murderhelp.domain.product.service.ProductService;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final CartService cartService;
    private final ProductCacheEvictionService productCacheEvictionService;
    private final PaymentService paymentService;

    @Transactional
    public CreateOrderResponse createOrder(Long memberId, CreateOrderRequest createOrderRequest) {
        // 요청 정보 및 주문 가능 여부 검증
        List<CartItemResponse> cartItemList = cartService.getItems(memberId, createOrderRequest.cartItemIds());
        if (cartItemList.size() != createOrderRequest.cartItemIds().size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 재고 확인 및 확보
        List<Long> productIds = cartItemList.stream().map(CartItemResponse::productId).toList();
        Map<Long, Product> productMap = productService.getProducts(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 재고 차감
        for (CartItemResponse cartItem : cartItemList) {
            productMap.get(cartItem.productId()).decreaseStock(cartItem.quantity());
        }

        // 주문 저장
        Order savedOrder = orderService.createOrder(
                memberId,
                createOrderRequest,
                cartItemList,
                productMap
        );

        // payment 생성
        paymentService.createPayment(savedOrder, savedOrder.getTotalAmount().intValue());

        productCacheEvictionService.evictProductCaches();

        return CreateOrderResponse.from(savedOrder);
    }


}
