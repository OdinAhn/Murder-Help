package org.example.murderhelp.domain.review.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.global.entity.BaseTimeEntity;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_reviews_order_item", columnNames = "order_item_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Integer rating;

    @Lob
    @Column(nullable = false)
    private String content;

    @Builder
    private  Review(Long orderItemId, Long productId, Long memberId, Integer rating, String content) {
        validateRating(rating);
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.memberId = memberId;
        this.rating = rating;
        this.content = content;
    }

    public void update(Integer rating, String content) {
        validateRating(rating);
        this.rating = rating;
        this.content = content;
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        }
    }
}