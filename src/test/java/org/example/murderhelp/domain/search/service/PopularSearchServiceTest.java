package org.example.murderhelp.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class PopularSearchServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private PopularSearchService popularSearchService;

    @BeforeEach
    void setUp() {
        popularSearchService = new PopularSearchService(stringRedisTemplate);
    }

    @Test
    void 최초_검색이면_정규화한_검색어의_점수를_증가시킨다() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        popularSearchService.recordSearch("1", "  PISTOL   Gun  ");

        String todayKey = "search:popular:daily:" + LocalDate.now();
        verify(zSetOperations).incrementScore(todayKey, "pistol gun", 1);
        verify(stringRedisTemplate).expire(todayKey, Duration.ofDays(8));
    }

    @Test
    void 같은_사용자의_중복_검색이면_점수를_증가시키지_않는다() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        popularSearchService.recordSearch("1", "pistol");

        verify(zSetOperations, never()).incrementScore(anyString(), anyString(), anyDouble());
    }

    @Test
    void 오늘의_검색어를_점수_내림차순_순위와_함께_반환한다() {
        Set<ZSetOperations.TypedTuple<String>> entries = new LinkedHashSet<>();
        entries.add(new DefaultTypedTuple<>("pistol", 12.0));
        entries.add(new DefaultTypedTuple<>("rifle", 8.0));
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores(anyString(), anyLong(), anyLong())).thenReturn(entries);

        assertThat(popularSearchService.getPopularSearches(2))
                .extracting(response -> response.rank() + ":" + response.keyword() + ":" + response.score())
                .containsExactly("1:pistol:12", "2:rifle:8");
    }
}
