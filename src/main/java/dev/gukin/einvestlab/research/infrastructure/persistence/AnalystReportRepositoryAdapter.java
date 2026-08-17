package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import dev.gukin.einvestlab.research.domain.CoveredStock;
import dev.gukin.einvestlab.research.domain.EpsExtractionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AnalystReportRepositoryAdapter implements AnalystReportRepository {

    private final AnalystReportJpaRepository jpa;

    @Override
    public AnalystReport save(AnalystReport analystReport) {
        return jpa.save(analystReport);
    }

    @Override
    public boolean existsByReportIdx(long reportIdx) {
        return jpa.existsByReportIdx(reportIdx);
    }

    @Override
    public List<AnalystReport> findAllWithoutPdf() {
        return jpa.findAllByPdfPathIsNullAndPdfPurgedAtIsNull();
    }

    @Override
    public List<AnalystReport> findAllPendingEpsExtraction() {
        return jpa.findAllPendingEpsExtraction(EpsExtractionStatus.FAILED);
    }

    @Override
    public List<AnalystReport> findAllWithPdfPublishedBefore(LocalDate cutoff) {
        return jpa.findAllByPdfPathIsNotNullAndPublishedDateBefore(cutoff);
    }

    @Override
    public List<CoveredStock> findRecentlyCovered(LocalDate since) {
        return jpa.findRecentlyCovered(since).stream()
                .map(row -> new CoveredStock(
                        row.getStockCode(),
                        row.getCompanyName(),
                        row.getReportCount(),
                        row.getBrokerCount(),
                        row.getLatestPublishedDate()))
                .toList();
    }

    @Override
    public Optional<AnalystReport> findByReportIdx(long reportIdx) {
        return jpa.findByReportIdx(reportIdx);
    }

    @Override
    public Optional<AnalystReport> findPreviousExtractedByBroker(String stockCode, String broker,
                                                                 LocalDate publishedDate, long reportIdx) {
        return jpa.findPreviousExtractedByBroker(stockCode, broker, publishedDate, reportIdx,
                        EpsExtractionStatus.EXTRACTED, PageRequest.of(0, 1))
                .stream().findFirst();
    }
}
