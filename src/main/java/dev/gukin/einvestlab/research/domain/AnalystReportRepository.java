package dev.gukin.einvestlab.research.domain;

import java.time.LocalDate;
import java.util.List;

public interface AnalystReportRepository {

    AnalystReport save(AnalystReport analystReport);

    boolean existsByReportIdx(long reportIdx);

    List<AnalystReport> findAllWithoutPdf();

    List<AnalystReport> findAllPendingEpsExtraction();

    List<CoveredStock> findRecentlyCovered(LocalDate since);
}
