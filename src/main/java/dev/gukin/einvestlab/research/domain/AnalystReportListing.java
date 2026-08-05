package dev.gukin.einvestlab.research.domain;

import java.time.LocalDate;

public record AnalystReportListing(
        long reportIdx,
        String stockCode,
        String companyName,
        String title,
        String broker,
        String authors,
        LocalDate publishedDate,
        Long targetPrice,
        String opinion
) {
}
