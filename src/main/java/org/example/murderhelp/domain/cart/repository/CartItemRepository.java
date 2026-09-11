package org.example.murderhelp.domain.cart.repository;

import org.example.murderhelp.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByMember_IdAndProduct_Id(Long memberId, Long productId);
}
