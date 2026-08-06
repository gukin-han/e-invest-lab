package dev.gukin.einvestlab.research.interfaces.web.dto;

import dev.gukin.einvestlab.research.application.AnalystReportEpsExtractResult;

public record AnalystReportEpsExtractResponse(int extracted, int noSummaryTable, int failed) {

    public static AnalystReportEpsExtractResponse from(AnalystReportEpsExtractResult result) {
        return new AnalystReportEpsExtractResponse(
                result.extracted(), result.noSummaryTable(), result.failed());
    }
}
