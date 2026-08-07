package dev.gukin.einvestlab.research.interfaces.web.dto;

import dev.gukin.einvestlab.research.domain.EpsConsensus;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record EpsConsensusResponse(
        int fiscalYear,
        BigDecimal averageEps,
        long sampleCount,
        BigDecimal minEps,
        BigDecimal maxEps
) {

    public static EpsConsensusResponse from(EpsConsensus consensus) {
        return new EpsConsensusResponse(
                consensus.fiscalYear(),
                consensus.averageEps().setScale(0, RoundingMode.HALF_UP),
                consensus.sampleCount(),
                consensus.minEps().setScale(0, RoundingMode.HALF_UP),
                consensus.maxEps().setScale(0, RoundingMode.HALF_UP));
    }
}
