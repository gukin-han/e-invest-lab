package dev.gukin.einvestlab.research.domain;

import java.math.BigDecimal;

public record EpsConsensus(
        int fiscalYear,
        BigDecimal averageEps,
        long sampleCount,
        BigDecimal minEps,
        BigDecimal maxEps
) {
}
