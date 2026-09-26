package dev.gukin.einvestlab.research.interfaces.web.dto;

import dev.gukin.einvestlab.research.domain.CoveredStock;

import java.time.LocalDate;

public record CoveredStockResponse(
        String stockCode,
        String companyName,
        long reportCount,
        long brokerCount,
        LocalDate latestPublishedDate
) {

    public static CoveredStockResponse from(CoveredStock coveredStock) {
        return new CoveredStockResponse(
                coveredStock.stockCode(),
                coveredStock.companyName(),
                coveredStock.reportCount(),
                coveredStock.brokerCount(),
                coveredStock.latestPublishedDate());
    }
}
