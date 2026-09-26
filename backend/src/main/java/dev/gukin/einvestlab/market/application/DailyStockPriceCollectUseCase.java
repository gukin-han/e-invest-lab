package dev.gukin.einvestlab.market.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.DailyStockPriceEntry;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.market.domain.DailyStockPriceSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class DailyStockPriceCollectUseCase {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LOOKBACK_DAYS = 7;

    private final DailyStockPriceSource source;
    private final DailyStockPriceRepository repository;
    private final TransactionTemplate dateTransaction;

    public DailyStockPriceCollectUseCase(DailyStockPriceSource source,
                                         DailyStockPriceRepository repository,
                                         PlatformTransactionManager transactionManager) {
        this.source = source;
        this.repository = repository;
        this.dateTransaction = new TransactionTemplate(transactionManager);
    }

    public DailyStockPriceCollectResult collect(LocalDate start, LocalDate end, Instant baseTime) {
        CollectWindow window = resolveWindow(start, end, baseTime);

        int upsertedPrices = 0;
        int tradedDays = 0;
        for (LocalDate date = window.start(); !date.isAfter(window.end()); date = date.plusDays(1)) {
            List<DailyStockPriceEntry> entries = source.fetchAll(date);
            if (entries.isEmpty()) {
                continue;
            }
            tradedDays++;
            List<DailyStockPrice> prices = entries.stream()
                    .map(entry -> toPrice(entry, baseTime))
                    .toList();
            upsertedPrices += dateTransaction.execute(status -> repository.upsertPrices(prices));
        }
        return new DailyStockPriceCollectResult(upsertedPrices, tradedDays);
    }

    private CollectWindow resolveWindow(LocalDate start, LocalDate end, Instant baseTime) {
        LocalDate effectiveEnd = end != null ? end : LocalDate.ofInstant(baseTime, KOREA);
        LocalDate effectiveStart = start != null ? start : effectiveEnd.minusDays(DEFAULT_LOOKBACK_DAYS);
        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new IllegalArgumentException(
                    "수집 기간 역전: " + effectiveStart + " > " + effectiveEnd);
        }
        return new CollectWindow(effectiveStart, effectiveEnd);
    }

    private record CollectWindow(LocalDate start, LocalDate end) {
    }

    private DailyStockPrice toPrice(DailyStockPriceEntry entry, Instant baseTime) {
        return DailyStockPrice.builder()
                .id(Ids.generate())
                .stockCode(entry.stockCode())
                .tradeDate(entry.tradeDate())
                .marketCategory(entry.marketCategory())
                .openPrice(entry.openPrice())
                .highPrice(entry.highPrice())
                .lowPrice(entry.lowPrice())
                .closePrice(entry.closePrice())
                .volume(entry.volume())
                .listedShareCount(entry.listedShareCount())
                .marketCap(entry.marketCap())
                .collectedAt(baseTime)
                .build();
    }
}
