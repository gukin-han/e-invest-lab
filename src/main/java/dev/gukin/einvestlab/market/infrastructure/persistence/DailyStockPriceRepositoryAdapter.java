package dev.gukin.einvestlab.market.infrastructure.persistence;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.market.domain.ShareCountTrend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DailyStockPriceRepositoryAdapter implements DailyStockPriceRepository {

    private final DailyStockPriceJpaRepository jpa;
    private final DailyStockPriceJdbcRepository jdbc;

    @Override
    public int upsertPrices(List<DailyStockPrice> prices) {
        return jdbc.upsertPrices(prices);
    }

    @Override
    public Optional<DailyStockPrice> findLatestByStockCode(String stockCode) {
        return jpa.findTopByStockCodeOrderByTradeDateDesc(stockCode);
    }

    @Override
    public List<DailyStockPrice> findSeries(String stockCode, LocalDate from, LocalDate to) {
        return jpa.findAllByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(stockCode, from, to);
    }

    @Override
    public int rebuildShareCountChanges(java.time.Instant computedAt) {
        return jdbc.rebuildShareCountChanges(computedAt);
    }

    @Override
    public List<ShareCountTrend> findShareCountTrends(LocalDate since, LocalDate listedCutoff,
                                                      boolean decreasing, BigDecimal maxSingleDropPct,
                                                      BigDecimal maxSingleRisePct, int limit) {
        return jdbc.findShareCountTrends(since, listedCutoff, decreasing,
                maxSingleDropPct, maxSingleRisePct, limit);
    }
}
