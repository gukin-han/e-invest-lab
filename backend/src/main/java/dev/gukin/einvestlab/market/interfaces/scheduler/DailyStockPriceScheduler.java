package dev.gukin.einvestlab.market.interfaces.scheduler;

import dev.gukin.einvestlab.market.application.DailyStockPriceCollectResult;
import dev.gukin.einvestlab.market.application.DailyStockPriceCollectUseCase;
import dev.gukin.einvestlab.market.application.ShareCountChangeRebuildUseCase;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStockPriceScheduler {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final DailyStockPriceCollectUseCase collectUseCase;
    private final ShareCountChangeRebuildUseCase rebuildUseCase;
    private final DailyStockPriceRepository repository;
    private final Clock clock;

    // 금융위 API 는 전일 데이터를 다음날 낮 어느 시점에 공표한다 (07시대엔 없음을 실측).
    // 공표 즉시 따라잡아 알림의 현재가 지연을 이틀 → 하루로 줄인다. 어제가 이미 있으면 API 호출 없이 반환.
    @Scheduled(cron = "0 0 9-17 * * MON-FRI", zone = "Asia/Seoul")
    void collectYesterdayIfMissing() {
        try {
            LocalDate yesterday = LocalDate.ofInstant(clock.instant(), KOREA).minusDays(1);
            if (repository.existsByTradeDate(yesterday)) {
                return;
            }
            DailyStockPriceCollectResult result =
                    collectUseCase.collect(yesterday, yesterday, clock.instant());
            if (result.tradedDays() > 0) {
                log.info("daily stock price intraday catch-up completed. tradeDate={} upsertedPrices={}",
                        yesterday, result.upsertedPrices());
            }
        } catch (Exception e) {
            log.error("daily stock price intraday catch-up failed.", e);
        }
    }

    @Scheduled(cron = "0 10 18 * * *", zone = "Asia/Seoul")
    void collectDaily() {
        try {
            log.info("daily stock price collect started.");
            DailyStockPriceCollectResult result = collectUseCase.collect(null, null, clock.instant());
            log.info("daily stock price collect completed. upsertedPrices={} tradedDays={}",
                    result.upsertedPrices(), result.tradedDays());
            int changes = rebuildUseCase.rebuild(clock.instant());
            log.info("share count change rebuild completed. affectedRows={}", changes);
        } catch (Exception e) {
            log.error("daily stock price collect failed.", e);
        }
    }
}
