package dev.gukin.einvestlab.research.domain;

import java.math.BigDecimal;

public record EpsFigure(int fiscalYear, boolean estimated, BigDecimal eps) {
}
