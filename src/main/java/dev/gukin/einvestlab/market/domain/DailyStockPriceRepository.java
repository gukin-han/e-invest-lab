package dev.gukin.einvestlab.market.domain;

import java.util.List;
import java.util.Optional;

public interface DailyStockPriceRepository {

    int upsertPrices(List<DailyStockPrice> prices);

    Optional<DailyStockPrice> findLatestByStockCode(String stockCode);
}
