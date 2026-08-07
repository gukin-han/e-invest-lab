package dev.gukin.einvestlab.market.interfaces.scheduler;

import dev.gukin.einvestlab.market.application.DailyStockPriceCollectResult;
import dev.gukin.einvestlab.market.application.DailyStockPriceCollectUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStockPriceScheduler {

    private final DailyStockPriceCollectUseCase collectUseCase;
    private final Clock clock;

    @Scheduled(cron = "0 10 18 * * *", zone = "Asia/Seoul")
    void collectDaily() {
        try {
            log.info("daily stock price collect started.");
            DailyStockPriceCollectResult result = collectUseCase.collect(null, null, clock.instant());
            log.info("daily stock price collect completed. upsertedPrices={} tradedDays={}",
                    result.upsertedPrices(), result.tradedDays());
        } catch (Exception e) {
            log.error("daily stock price collect failed.", e);
        }
    }
}
