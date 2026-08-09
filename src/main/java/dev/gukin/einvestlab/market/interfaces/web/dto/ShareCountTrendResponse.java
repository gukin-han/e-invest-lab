package dev.gukin.einvestlab.market.interfaces.web.dto;

import dev.gukin.einvestlab.market.domain.ShareCountTrend;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShareCountTrendResponse(
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

    public static ShareCountTrendResponse from(ShareCountTrend trend) {
        return new ShareCountTrendResponse(
                trend.stockCode(),
                trend.companyName(),
                trend.startCount(),
                trend.endCount(),
                trend.netChangePct(),
                trend.decreaseEvents(),
                trend.decreasedShares(),
                trend.lastDecreaseDate(),
                trend.maxDropPct(),
                trend.marketCap());
    }
}
