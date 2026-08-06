package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import dev.gukin.einvestlab.research.domain.EpsExtractionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

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
        return jpa.findAllByPdfPathIsNull();
    }

    @Override
    public List<AnalystReport> findAllPendingEpsExtraction() {
        return jpa.findAllPendingEpsExtraction(EpsExtractionStatus.FAILED);
    }
}
