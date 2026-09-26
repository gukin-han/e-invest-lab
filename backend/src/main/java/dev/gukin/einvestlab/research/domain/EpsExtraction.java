package dev.gukin.einvestlab.research.domain;

import java.util.List;

public record EpsExtraction(EpsExtractionStatus status, List<EpsFigure> figures) {

    public static EpsExtraction extracted(List<EpsFigure> figures) {
        return new EpsExtraction(EpsExtractionStatus.EXTRACTED, List.copyOf(figures));
    }

    public static EpsExtraction noSummaryTable() {
        return new EpsExtraction(EpsExtractionStatus.NO_SUMMARY_TABLE, List.of());
    }

    public static EpsExtraction failed() {
        return new EpsExtraction(EpsExtractionStatus.FAILED, List.of());
    }
}
