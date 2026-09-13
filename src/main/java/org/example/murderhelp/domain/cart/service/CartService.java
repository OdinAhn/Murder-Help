package org.example.murderhelp.domain.cart.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.cart.dto.CartItemAddRequest;
import org.example.murderhelp.domain.cart.dto.CartItemDetailResponse;
import org.example.murderhelp.domain.cart.dto.CartItemUpdateRequest;
import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.cart.entity.CartItem;
import org.example.murderhelp.domain.cart.repository.CartItemRepository;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.example.murderhelp.domain.product.repository.ProductRepository;
import org.example.murderhelp.global.config.cache.CacheNames;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final CartCacheEvictionService cartCacheEvictionService;

    @Transactional
    public CartItemResponse addItem(Long memberId, CartItemAddRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.validateAccessibleBy(ProductTier.valueOf(member.getGrade().name()));

        CartItem cartItem = cartItemRepository
                .findByMember_IdAndProduct_Id(memberId, product.getId())
                .map(existingItem -> {
                    existingItem.addQuantity(request.quantity());
                    return existingItem;
                })
                .orElseGet(() -> CartItem.create(member, product, request.quantity()));

        CartItemResponse response = CartItemResponse.from(cartItemRepository.save(cartItem));
        cartCacheEvictionService.evictCartItems(memberId);
        return response;
    }

    @Cacheable(cacheNames = CacheNames.CART_ITEMS, key = "'member:' + #memberId")
    @Transactional(readOnly = true)
    public List<CartItemDetailResponse> getItems(Long memberId) {
        return cartItemRepository.findAllByMember_IdOrderByCreatedAtAsc(memberId).stream()
                .map(CartItemDetailResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getItems(Long memberId, List<Long> cartIds) {
        List<Long> requestedIds = getDistinctIds(cartIds);
        if (requestedIds.isEmpty()) {
            return List.of();
        }

        List<CartItem> cartItems = getOwnedItems(memberId, requestedIds);
        Map<Long, CartItem> cartItemById = cartItems.stream()
                .collect(Collectors.toMap(CartItem::getId, Function.identity()));

        return requestedIds.stream()
                .map(cartItemById::get)
                .map(CartItemResponse::from)
                .toList();
    }

    @Transactional
    public CartItemResponse updateItemQuantity(
            Long memberId,
            Long cartItemId,
            CartItemUpdateRequest request
    ) {
        CartItem cartItem = getOwnedItem(memberId, cartItemId);
        cartItem.changeQuantity(request.quantity());
        CartItemResponse response = CartItemResponse.from(cartItem);
        cartCacheEvictionService.evictCartItems(memberId);
        return response;
    }

    @Transactional
    public void deleteItem(Long memberId, Long cartItemId) {
        CartItem cartItem = getOwnedItem(memberId, cartItemId);
        cartItemRepository.delete(cartItem);
        cartCacheEvictionService.evictCartItems(memberId);
    }

    @Transactional
    public void deleteItems(Long memberId, List<Long> cartItemIds) {
        List<Long> requestedIds = getDistinctIds(cartItemIds);
        if (requestedIds.isEmpty()) {
            return;
        }

        List<CartItem> cartItems = getOwnedItems(memberId, requestedIds);
        cartItemRepository.deleteAllInBatch(cartItems);
        cartCacheEvictionService.evictCartItems(memberId);
    }

    private CartItem getOwnedItem(Long memberId, Long cartItemId) {
        return cartItemRepository.findByIdAndMember_Id(cartItemId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    private List<CartItem> getOwnedItems(Long memberId, List<Long> cartItemIds) {
        List<CartItem> cartItems = cartItemRepository.findAllByMember_IdAndIdIn(memberId, cartItemIds);
        if (cartItems.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return cartItems;
    }

    private List<Long> getDistinctIds(List<Long> ids) {
        if (ids == null || ids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("장바구니 상품 ID는 필수입니다.");
        }
        return ids.stream().distinct().toList();
    }
}
