package dev.gukin.einvestlab.research.domain;

import java.util.List;

public interface AnalystReportRepository {

    AnalystReport save(AnalystReport analystReport);

    boolean existsByReportIdx(long reportIdx);

    List<AnalystReport> findAllWithoutPdf();

    List<AnalystReport> findAllPendingEpsExtraction();
}
