package org.example.murderhelp.domain.search.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.murderhelp.domain.search.dto.PopularSearchResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularSearchService {

    private static final String POPULAR_SEARCH_KEY_PREFIX = "search:popular:daily:";
    private static final String DEDUPLICATION_KEY_PREFIX = "search:dedupe:";
    private static final Duration DEDUPLICATION_TTL = Duration.ofMinutes(5);
    private static final Duration POPULAR_SEARCH_TTL = Duration.ofDays(8);

    private final StringRedisTemplate stringRedisTemplate;

    public void recordSearch(String memberId, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String deduplicationKey = DEDUPLICATION_KEY_PREFIX + memberId + ":" + normalizedKeyword;

        try {
            Boolean firstSearch = stringRedisTemplate.opsForValue()
                    .setIfAbsent(deduplicationKey, "1", DEDUPLICATION_TTL);

            if (!Boolean.TRUE.equals(firstSearch)) {
                return;
            }

            String popularSearchKey = getPopularSearchKey();
            stringRedisTemplate.opsForZSet().incrementScore(popularSearchKey, normalizedKeyword, 1);
            stringRedisTemplate.expire(popularSearchKey, POPULAR_SEARCH_TTL);
        } catch (DataAccessException exception) {
            log.warn("인기 검색어 집계에 실패했습니다.", exception);
        }
    }

    public List<PopularSearchResponse> getPopularSearches(int limit) {
        try {
            Set<ZSetOperations.TypedTuple<String>> entries = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(getPopularSearchKey(), 0, limit - 1);

            if (entries == null) {
                return List.of();
            }

            int[] rank = {1};
            return entries.stream()
                    .filter(entry -> entry.getValue() != null)
                    .map(entry -> new PopularSearchResponse(
                            rank[0]++,
                            entry.getValue(),
                            entry.getScore() == null ? 0L : entry.getScore().longValue()
                    ))
                    .toList();
        } catch (DataAccessException exception) {
            log.warn("인기 검색어 조회에 실패했습니다.", exception);
            return List.of();
        }
    }

    private String getPopularSearchKey() {
        return POPULAR_SEARCH_KEY_PREFIX + LocalDate.now();
    }

    private String normalizeKeyword(String keyword) {
        return keyword.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
