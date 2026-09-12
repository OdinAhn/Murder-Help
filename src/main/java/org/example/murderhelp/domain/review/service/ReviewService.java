package org.example.murderhelp.domain.review.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.order.entity.OrderItem;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.order.repository.OrderItemRepository;
import org.example.murderhelp.domain.review.dto.ReviewCreateRequest;
import org.example.murderhelp.domain.review.dto.ReviewResponse;
import org.example.murderhelp.domain.review.dto.ReviewUpdateRequest;
import org.example.murderhelp.domain.review.entity.Review;
import org.example.murderhelp.domain.review.repository.ReviewRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;

    public List<ReviewResponse> getMyReviews(Long memberId) {
        return reviewRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse createReview(Long memberId, ReviewCreateRequest request) {
        OrderItem orderItem = orderItemRepository.findById(request.orderItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // getMemberId() 부분은 현재 주문 엔티티 필드명에 맞춰 수정
        if (!orderItem.getOrder().getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (orderItem.getOrder().getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "구매 확정된 상품만 리뷰를 작성할 수 있습니다."
            );
        }

        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "이미 작성한 리뷰가 있습니다."
            );
        }

        Review review = Review.builder()
                .orderItemId(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .memberId(memberId)
                .rating(request.rating())
                .content(request.content())
                .build();

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateReview(
            Long memberId,
            Long reviewId,
            ReviewUpdateRequest request
    ) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.update(request.rating(), request.content());

        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteReview(Long memberId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        reviewRepository.delete(review);
    }
}