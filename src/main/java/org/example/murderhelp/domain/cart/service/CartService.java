package org.example.murderhelp.domain.cart.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.cart.dto.CartItemAddRequest;
import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.cart.entity.CartItem;
import org.example.murderhelp.domain.cart.repository.CartItemRepository;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.example.murderhelp.domain.product.repository.ProductRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

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

        return CartItemResponse.from(cartItemRepository.save(cartItem));
    }
}
