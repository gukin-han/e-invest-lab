package dev.gukin.einvestlab.research.interfaces.web.dto;

import dev.gukin.einvestlab.research.application.EpsConsensusResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public record EpsConsensusResponse(
        String stockCode,
        Integer latestClosePrice,
        LocalDate latestTradeDate,
        List<YearlyConsensusResponse> years
) {

    public record YearlyConsensusResponse(
            int fiscalYear,
            BigDecimal averageEps,
            long sampleCount,
            BigDecimal minEps,
            BigDecimal maxEps,
            BigDecimal forwardPer
    ) {
    }

    public static EpsConsensusResponse from(EpsConsensusResult result) {
        return new EpsConsensusResponse(
                result.stockCode(),
                result.latestClosePrice(),
                result.latestTradeDate(),
                result.years().stream()
                        .map(EpsConsensusResponse::fromYear)
                        .toList());
    }

    private static YearlyConsensusResponse fromYear(EpsConsensusResult.YearlyConsensus year) {
        return new YearlyConsensusResponse(
                year.fiscalYear(),
                year.averageEps().setScale(0, RoundingMode.HALF_UP),
                year.sampleCount(),
                year.minEps().setScale(0, RoundingMode.HALF_UP),
                year.maxEps().setScale(0, RoundingMode.HALF_UP),
                year.forwardPer());
    }
}
