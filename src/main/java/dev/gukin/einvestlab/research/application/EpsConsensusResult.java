package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.research.domain.EpsConsensus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public record EpsConsensusResult(
        String stockCode,
        Integer latestClosePrice,
        LocalDate latestTradeDate,
        List<YearlyConsensus> years
) {

    public record YearlyConsensus(
            int fiscalYear,
            BigDecimal averageEps,
            long sampleCount,
            BigDecimal minEps,
            BigDecimal maxEps,
            BigDecimal forwardPer
    ) {
    }

    public static EpsConsensusResult of(String stockCode, DailyStockPrice latestPrice,
                                        List<EpsConsensus> consensuses) {
        Integer closePrice = latestPrice != null ? latestPrice.getClosePrice() : null;
        LocalDate tradeDate = latestPrice != null ? latestPrice.getTradeDate() : null;
        List<YearlyConsensus> years = consensuses.stream()
                .map(consensus -> new YearlyConsensus(
                        consensus.fiscalYear(),
                        consensus.averageEps(),
                        consensus.sampleCount(),
                        consensus.minEps(),
                        consensus.maxEps(),
                        forwardPer(closePrice, consensus.averageEps())))
                .toList();
        return new EpsConsensusResult(stockCode, closePrice, tradeDate, years);
    }

    private static BigDecimal forwardPer(Integer closePrice, BigDecimal averageEps) {
        if (closePrice == null || averageEps.signum() <= 0) {
            return null;
        }
        return BigDecimal.valueOf(closePrice).divide(averageEps, 2, RoundingMode.HALF_UP);
    }
}
