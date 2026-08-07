package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;

import java.util.ArrayList;
import java.util.List;

class StubAnalystReportRepository implements AnalystReportRepository {

    Long existingReportIdx;
    List<AnalystReport> withoutPdf = List.of();
    List<AnalystReport> pendingEpsExtraction = List.of();
    final List<AnalystReport> saved = new ArrayList<>();

    @Override
    public AnalystReport save(AnalystReport analystReport) {
        saved.add(analystReport);
        return analystReport;
    }

    @Override
    public boolean existsByReportIdx(long reportIdx) {
        return existingReportIdx != null && existingReportIdx == reportIdx;
    }

    @Override
    public List<AnalystReport> findAllWithoutPdf() {
        return withoutPdf;
    }

    @Override
    public List<AnalystReport> findAllPendingEpsExtraction() {
        return pendingEpsExtraction;
    }
}
