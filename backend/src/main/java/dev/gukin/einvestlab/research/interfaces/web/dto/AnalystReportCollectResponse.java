package dev.gukin.einvestlab.research.interfaces.web.dto;

import dev.gukin.einvestlab.research.application.AnalystReportCollectResult;

public record AnalystReportCollectResponse(int collected, int skipped) {

    public static AnalystReportCollectResponse from(AnalystReportCollectResult result) {
        return new AnalystReportCollectResponse(result.collected(), result.skipped());
    }
}
