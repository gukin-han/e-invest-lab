package dev.gukin.einvestlab.research.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EpsRevision(
        LocalDate publishedDate,
        String broker,
        BigDecimal eps,
        boolean estimated
) {
}
