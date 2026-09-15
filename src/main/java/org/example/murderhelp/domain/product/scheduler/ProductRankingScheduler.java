package org.example.murderhelp.domain.product.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.murderhelp.domain.product.service.ProductRankingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRankingScheduler {

    private final ProductRankingService rankingService;

    /**
     * 매일 새벽 3시에 주간(최근 7일) 베스트 무기 랭킹을 갱신합니다.
     * ShedLock을 통해 다중 서버(Scale-Out) 환경에서도 단 하나의 서버에서만 중복 없이 실행되도록 보장합니다.
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시에 실행
   // @Scheduled(cron = "0 */10 * * * *") // 10분마다 실행
    @SchedulerLock(
            name = "updateWeeklyBestProductsLock", 
            lockAtLeastFor = "1m",   // 최소 1분 동안은 다른 서버가 락을 뺏을 수 없음
            lockAtMostFor = "5m"     // 서버가 죽더라도 5분 뒤에는 락을 해제(Deadlock 방지)
    )
    public void runWeeklyBestUpdate() {
        log.info("[ShedLock] 락 획득 성공! 주간 베스트 무기 랭킹 업데이트 스케줄러를 시작합니다.");
        rankingService.updateWeeklyBestProducts();
        log.info("[ShedLock] 주간 베스트 무기 랭킹 업데이트 완료!");
    }
}
