package org.example.murderhelp.domain.cart.service;

import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.cart.entity.CartItem;
import org.example.murderhelp.domain.cart.repository.CartItemRepository;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.repository.ProductRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void 요청한_장바구니_상품을_입력한_ID_순서대로_조회한다() {
        Long memberId = 1L;
        CartItem firstItem = cartItem(10L, 100L, 2);
        CartItem secondItem = cartItem(20L, 200L, 3);
        when(cartItemRepository.findAllByMember_IdAndIdIn(memberId, List.of(20L, 10L)))
                .thenReturn(List.of(firstItem, secondItem));

        List<CartItemResponse> result = cartService.getItems(memberId, List.of(20L, 10L));

        assertThat(result).containsExactly(
                new CartItemResponse(20L, 200L, 3),
                new CartItemResponse(10L, 100L, 2)
        );
    }

    @Test
    void 요청한_ID가_없거나_다른_회원의_상품이면_조회에_실패한다() {
        Long memberId = 1L;
        CartItem cartItem = cartItem(10L, 100L, 2);
        when(cartItemRepository.findAllByMember_IdAndIdIn(memberId, List.of(10L, 20L)))
                .thenReturn(List.of(cartItem));

        assertThatThrownBy(() -> cartService.getItems(memberId, List.of(10L, 20L)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Test
    void 소유한_장바구니_상품을_모두_삭제한다() {
        Long memberId = 1L;
        CartItem firstItem = cartItem(10L, 100L, 2);
        CartItem secondItem = cartItem(20L, 200L, 3);
        when(cartItemRepository.findAllByMember_IdAndIdIn(memberId, List.of(10L, 20L)))
                .thenReturn(List.of(firstItem, secondItem));

        cartService.deleteItems(memberId, List.of(10L, 20L));

        verify(cartItemRepository).deleteAllInBatch(List.of(firstItem, secondItem));
    }

    @Test
    void 삭제할_ID_중_하나라도_소유하지_않으면_아무것도_삭제하지_않는다() {
        Long memberId = 1L;
        CartItem cartItem = cartItem(10L, 100L, 2);
        when(cartItemRepository.findAllByMember_IdAndIdIn(memberId, List.of(10L, 20L)))
                .thenReturn(List.of(cartItem));

        assertThatThrownBy(() -> cartService.deleteItems(memberId, List.of(10L, 20L)))
                .isInstanceOf(BusinessException.class);
        verify(cartItemRepository, never()).deleteAllInBatch(org.mockito.ArgumentMatchers.anyList());
    }

    private CartItem cartItem(Long cartItemId, Long productId, int quantity) {
        CartItem cartItem = org.mockito.Mockito.mock(CartItem.class);
        Product product = org.mockito.Mockito.mock(Product.class);
        lenient().when(cartItem.getId()).thenReturn(cartItemId);
        lenient().when(cartItem.getProduct()).thenReturn(product);
        lenient().when(cartItem.getQuantity()).thenReturn(quantity);
        lenient().when(product.getId()).thenReturn(productId);
        return cartItem;
    }
}
