package dev.gukin.einvestlab.research.interfaces.web.dto;

import dev.gukin.einvestlab.research.application.AnalystReportPdfDownloadResult;

public record AnalystReportPdfDownloadResponse(int downloaded, int failed) {

    public static AnalystReportPdfDownloadResponse from(AnalystReportPdfDownloadResult result) {
        return new AnalystReportPdfDownloadResponse(result.downloaded(), result.failed());
    }
}
