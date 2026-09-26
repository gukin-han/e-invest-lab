package dev.gukin.einvestlab.research.interfaces.web.dto;

import dev.gukin.einvestlab.research.application.AnalystReportPdfPurgeResult;

public record AnalystReportPdfPurgeResponse(int purged, int failed) {

    public static AnalystReportPdfPurgeResponse from(AnalystReportPdfPurgeResult result) {
        return new AnalystReportPdfPurgeResponse(result.purged(), result.failed());
    }
}
