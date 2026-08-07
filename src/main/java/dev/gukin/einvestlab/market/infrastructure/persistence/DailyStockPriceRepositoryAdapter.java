package dev.gukin.einvestlab.market.infrastructure.persistence;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
