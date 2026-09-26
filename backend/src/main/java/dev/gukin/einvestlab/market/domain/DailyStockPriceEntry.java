package dev.gukin.einvestlab.market.domain;

import java.time.LocalDate;

public record DailyStockPriceEntry(
        String stockCode,
        LocalDate tradeDate,
        String marketCategory,
        int openPrice,
        int highPrice,
        int lowPrice,
        int closePrice,
        long volume,
        Long listedShareCount,
        Long marketCap
) {
}
