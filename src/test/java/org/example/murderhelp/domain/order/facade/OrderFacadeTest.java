package org.example.murderhelp.domain.order.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.cart.service.CartService;
import org.example.murderhelp.domain.order.dto.CreateOrderRequest;
import org.example.murderhelp.domain.order.dto.CreateOrderResponse;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.order.service.OrderService;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.service.ProductCacheEvictionService;
import org.example.murderhelp.domain.product.service.ProductService;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
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
    private CartService cartService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductCacheEvictionService productCacheEvictionService;

    @Mock
    private Product product;

    private final Long memberId = 7L;
    private final CreateOrderRequest request = new CreateOrderRequest(
            List.of(10L),
            "수령인",
            "010-1234-5678",
            "서울",
            "문 앞"
    );
    private final List<CartItemResponse> cartItems = List.of(new CartItemResponse(10L, 100L, 2));

    @Test
    @DisplayName("재고를 차감하고 주문 저장 후 장바구니와 상품 캐시를 갱신한다")
    void shouldDecreaseStockAndSaveOrderBeforeEvictingCaches() {
        givenOrderableCart();
        CreateOrderResponse response = new CreateOrderResponse(1L, "ORD-TEST", OrderStatus.PENDING_PAYMENT, 2000L);
        when(orderService.createOrder(memberId, request, cartItems, Map.of(100L, product)))
                .thenReturn(response);

        assertThat(orderFacade.createOrder(memberId, request)).isSameAs(response);

        var sequence = inOrder(cartService, productService, product, orderService, productCacheEvictionService);
        sequence.verify(cartService).getItems(memberId, request.cartItemIds());
        sequence.verify(productService).getProducts(List.of(100L));
        sequence.verify(product).decreaseStock(2);
        sequence.verify(orderService).createOrder(memberId, request, cartItems, Map.of(100L, product));
        sequence.verify(cartService).deleteItems(memberId, request.cartItemIds());
        sequence.verify(productCacheEvictionService).evictProductCaches();
    }

    @Test
    @DisplayName("장바구니 검증에 실패하면 주문을 진행하지 않는다")
    void shouldStopOrderWhenCartValidationFails() {
        BusinessException failure = new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        when(cartService.getItems(memberId, request.cartItemIds())).thenThrow(failure);

        assertThatThrownBy(() -> orderFacade.createOrder(memberId, request)).isSameAs(failure);

        verifyNoInteractions(productService, orderService, productCacheEvictionService);
        verify(cartService, never()).deleteItems(anyLong(), anyList());
    }

    @Test
    @DisplayName("요청한 장바구니 개수와 조회 개수가 다르면 주문을 진행하지 않는다")
    void shouldStopOrderWhenCartItemCountDoesNotMatch() {
        when(cartService.getItems(memberId, request.cartItemIds())).thenReturn(List.of());

        assertThatThrownBy(() -> orderFacade.createOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);

        verifyNoInteractions(productService, orderService, productCacheEvictionService);
        verify(cartService, never()).deleteItems(anyLong(), anyList());
    }

    @Test
    @DisplayName("재고 차감에 실패하면 주문 저장과 장바구니 삭제를 하지 않는다")
    void shouldNotSaveOrderOrDeleteCartItemsWhenStockDecreaseFails() {
        givenOrderableCart();
        IllegalStateException failure = new IllegalStateException("상품 재고가 부족합니다.");
        doThrow(failure).when(product).decreaseStock(2);

        assertThatThrownBy(() -> orderFacade.createOrder(memberId, request)).isSameAs(failure);

        verifyNoInteractions(orderService, productCacheEvictionService);
        verify(cartService, never()).deleteItems(anyLong(), anyList());
    }

    @Test
    @DisplayName("주문 저장에 실패하면 장바구니를 삭제하지 않는다")
    void shouldNotDeleteCartItemsWhenOrderSaveFails() {
        givenOrderableCart();
        IllegalStateException failure = new IllegalStateException("주문 저장 실패");
        when(orderService.createOrder(memberId, request, cartItems, Map.of(100L, product)))
                .thenThrow(failure);

        assertThatThrownBy(() -> orderFacade.createOrder(memberId, request)).isSameAs(failure);

        verifyNoInteractions(productCacheEvictionService);
        verify(cartService, never()).deleteItems(anyLong(), anyList());
    }

    private void givenOrderableCart() {
        when(cartService.getItems(memberId, request.cartItemIds())).thenReturn(cartItems);
        when(productService.getProducts(List.of(100L))).thenReturn(List.of(product));
        when(product.getId()).thenReturn(100L);
    }
}
