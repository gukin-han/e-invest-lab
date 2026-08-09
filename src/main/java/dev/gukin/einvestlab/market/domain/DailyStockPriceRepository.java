package dev.gukin.einvestlab.market.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyStockPriceRepository {

    int upsertPrices(List<DailyStockPrice> prices);

    Optional<DailyStockPrice> findLatestByStockCode(String stockCode);

    List<DailyStockPrice> findSeries(String stockCode, LocalDate from, LocalDate to);

    int rebuildShareCountChanges(java.time.Instant computedAt);

    List<ShareCountTrend> findShareCountTrends(LocalDate since, LocalDate listedCutoff,
                                               boolean decreasing, BigDecimal maxSingleDropPct,
                                               BigDecimal maxSingleRisePct, int limit);
}
