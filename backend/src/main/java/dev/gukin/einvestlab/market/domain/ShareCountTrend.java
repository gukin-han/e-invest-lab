package dev.gukin.einvestlab.market.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShareCountTrend(
        String stockCode,
        String companyName,
        long startCount,
        long endCount,
        BigDecimal netChangePct,
        int decreaseEvents,
        long decreasedShares,
        LocalDate lastDecreaseDate,
        BigDecimal maxDropPct,
        Long marketCap
) {
}
