package dev.gukin.einvestlab.research.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalystReportRepository {

    AnalystReport save(AnalystReport analystReport);

    boolean existsByReportIdx(long reportIdx);

    List<AnalystReport> findAllWithoutPdf();

    List<AnalystReport> findAllPendingEpsExtraction();

    List<AnalystReport> findAllWithPdfPublishedBefore(LocalDate cutoff);

    List<CoveredStock> findRecentlyCovered(LocalDate since);

    Optional<AnalystReport> findByReportIdx(long reportIdx);

    Optional<AnalystReport> findPreviousExtractedByBroker(String stockCode, String broker,
                                                          LocalDate publishedDate, long reportIdx);
}
