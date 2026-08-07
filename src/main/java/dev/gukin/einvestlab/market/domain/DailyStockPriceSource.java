package dev.gukin.einvestlab.market.domain;

import java.time.LocalDate;
import java.util.List;

public interface DailyStockPriceSource {

    List<DailyStockPriceEntry> fetchAll(LocalDate tradeDate);
}
