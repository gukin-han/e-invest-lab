package dev.gukin.einvestlab.research.domain;

import java.time.LocalDate;

public record CoveredStock(
        String stockCode,
        String companyName,
        long reportCount,
        long brokerCount,
        LocalDate latestPublishedDate
) {
}
