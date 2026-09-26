package dev.gukin.einvestlab.research.interfaces.web.dto;

import dev.gukin.einvestlab.research.domain.EpsRevision;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EpsRevisionResponse(
        LocalDate publishedDate,
        String broker,
        BigDecimal eps,
        boolean estimated
) {

    public static EpsRevisionResponse from(EpsRevision revision) {
        return new EpsRevisionResponse(
                revision.publishedDate(),
                revision.broker(),
                revision.eps(),
                revision.estimated());
    }
}
