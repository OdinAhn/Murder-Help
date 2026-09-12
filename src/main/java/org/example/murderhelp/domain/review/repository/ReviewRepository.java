package org.example.murderhelp.domain.review.repository;

import java.util.List;
import java.util.Optional;
import org.example.murderhelp.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByOrderItemId(Long orderItemId);

    List<Review> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    boolean existsByOrderItemId(Long orderItemId);
}