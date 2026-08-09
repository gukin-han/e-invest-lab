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

    public enum PerType { TRAILING, FORWARD }

    public record YearlyConsensus(
            int fiscalYear,
            BigDecimal averageEps,
            long sampleCount,
            BigDecimal minEps,
            BigDecimal maxEps,
            BigDecimal per,
            PerType perType
    ) {
    }

    public static EpsConsensusResult of(String stockCode, DailyStockPrice latestPrice,
                                        List<EpsConsensus> consensuses, int currentYear) {
        Integer closePrice = latestPrice != null ? latestPrice.getClosePrice() : null;
        LocalDate tradeDate = latestPrice != null ? latestPrice.getTradeDate() : null;
        List<YearlyConsensus> years = consensuses.stream()
                .map(consensus -> {
                    BigDecimal per = per(closePrice, consensus.averageEps());
                    return new YearlyConsensus(
                            consensus.fiscalYear(),
                            consensus.averageEps(),
                            consensus.sampleCount(),
                            consensus.minEps(),
                            consensus.maxEps(),
                            per,
                            per == null ? null : perType(consensus.fiscalYear(), currentYear));
                })
                .toList();
        return new EpsConsensusResult(stockCode, closePrice, tradeDate, years);
    }

    private static BigDecimal per(Integer closePrice, BigDecimal averageEps) {
        if (closePrice == null || averageEps.signum() <= 0) {
            return null;
        }
        return BigDecimal.valueOf(closePrice).divide(averageEps, 2, RoundingMode.HALF_UP);
    }

    private static PerType perType(int fiscalYear, int currentYear) {
        return fiscalYear < currentYear ? PerType.TRAILING : PerType.FORWARD;
    }
}
