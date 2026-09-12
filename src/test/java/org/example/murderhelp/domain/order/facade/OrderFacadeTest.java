package org.example.murderhelp.domain.order.facade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.order.dto.CreateOrderRequest;
import org.example.murderhelp.domain.order.dto.CreateOrderResponse;
import org.example.murderhelp.domain.order.service.MockupService;
import org.example.murderhelp.domain.order.service.OrderService;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.service.ProductCacheEvictionService;
import org.example.murderhelp.domain.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @InjectMocks
    private OrderFacade orderFacade;

    @Mock
    private MockupService mockupService;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private ProductCacheEvictionService productCacheEvictionService;

    @Mock
    private Product product;

    @Test
    void 주문이_성공하면_상품_캐시를_무효화한다() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(10L), "수령인", "010-0000-0000", "서울", null
        );
        CartItemResponse cartItem = new CartItemResponse(10L, 101L, 2);

        when(mockupService.getCartList(1L, request.cartItemIds())).thenReturn(List.of(cartItem));
        when(mockupService.getProductByIds(List.of(101L))).thenReturn(List.of(product));
        when(product.getId()).thenReturn(101L);
        when(orderService.createOrder(eq(1L), eq(request), any(), any()))
                .thenReturn(new CreateOrderResponse(1L, "ORD-001", null, 10_000L));

        orderFacade.createOrder(1L, request);

        verify(product).decreaseStock(2);
        verify(mockupService).deleteCartItems(request.cartItemIds());
        verify(productCacheEvictionService).evictProductCaches();
    }
}
