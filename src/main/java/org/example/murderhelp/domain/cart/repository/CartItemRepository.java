package org.example.murderhelp.domain.cart.repository;

import org.example.murderhelp.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByMember_IdAndProduct_Id(Long memberId, Long productId);

    @EntityGraph(attributePaths = {"product", "product.category", "product.category.parent"})
    List<CartItem> findAllByMember_IdOrderByCreatedAtAsc(Long memberId);

    Optional<CartItem> findByIdAndMember_Id(Long id, Long memberId);
}
