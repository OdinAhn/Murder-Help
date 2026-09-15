package org.example.murderhelp.domain.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.order.entity.OrderStatus;
import org.example.murderhelp.domain.order.repository.OrderItemRepository;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingService {

    private final OrderItemRepository orderItemRepository;
    private final StringRedisTemplate redisTemplate;

    public static final String RANKING_TARGET_KEY_PREFIX = "ranking:weekly:best:";
    private static final String RANKING_TEMP_KEY_PREFIX = "ranking:weekly:best:temp:";

    @Transactional(readOnly = true)
    public void updateWeeklyBestProducts() {
        log.info("[랭킹 스케줄러] 주간 베스트 무기 랭킹(등급별) 집계를 시작합니다.");
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);

        for (ProductTier userTier : ProductTier.values()) {
            if (userTier == ProductTier.GREEN) {
                continue; // GREEN 등급은 관리자 전용이므로 인기 상품 집계에서 제외합니다.
            }

            // 해당 등급의 유저가 볼 수 있는 무기 등급 리스트
            List<ProductTier> allowedTiers = Arrays.stream(ProductTier.values())
                    .filter(userTier::canAccess)
                    .collect(Collectors.toList());

            List<Object[]> topSelling = orderItemRepository.findTopSellingProductsByTiersSince(
                    startDate, 
                    OrderStatus.DELIVERED, 
                    allowedTiers,
                    PageRequest.of(0, 5)
            );

            String targetKey = RANKING_TARGET_KEY_PREFIX + userTier.name().toLowerCase();
            String tempKey = RANKING_TEMP_KEY_PREFIX + userTier.name().toLowerCase();

            if (topSelling.isEmpty()) {
                log.info("[랭킹 스케줄러] 최근 7일간 판매된 데이터가 없어 {} 등급 기존 랭킹 캐시를 초기화합니다.", userTier.name());
                redisTemplate.delete(targetKey);
                continue;
            }

            List<String> productIds = topSelling.stream()
                    .map(row -> String.valueOf(row[0]))
                    .collect(Collectors.toList());

            redisTemplate.delete(tempKey);
            redisTemplate.opsForList().rightPushAll(tempKey, productIds);
            redisTemplate.rename(tempKey, targetKey);
            
            log.info("[랭킹 스케줄러] {} 등급 주간 베스트 무기 랭킹 갱신 완료! (상품 ID: {})", userTier.name(), productIds);
        }
    }
}
