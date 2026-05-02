package dev.gukin.einvestlab.market.interfaces.scheduler;

import dev.gukin.einvestlab.market.application.service.StockPriceService;
import dev.gukin.einvestlab.market.domain.repository.MarketReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceScheduler {

    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 0);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StockPriceService stockPriceService;
    private final MarketReactionRepository marketReactionRepository;

//    @Scheduled(fixedDelayString = "${kis.polling.interval:300000}")
    public void collectPrices() {
        if (!isMarketOpen()) {
            return;
        }

        List<String> stockCodes = marketReactionRepository.findAll().stream()
                .map(r -> r.getStockCode())
                .distinct()
                .toList();

        if (stockCodes.isEmpty()) {
            return;
        }

        log.info("장중 주가 수집 시작: {}종목", stockCodes.size());

        for (String stockCode : stockCodes) {
            try {
                stockPriceService.fetchAndSave(stockCode);
            } catch (Exception e) {
                log.error("주가 수집 실패: stockCode={}", stockCode, e);
            }
        }
    }

    boolean isMarketOpen() {
        LocalTime now = LocalTime.now(KST);
        return !now.isBefore(MARKET_OPEN) && !now.isAfter(MARKET_CLOSE);
    }
}
