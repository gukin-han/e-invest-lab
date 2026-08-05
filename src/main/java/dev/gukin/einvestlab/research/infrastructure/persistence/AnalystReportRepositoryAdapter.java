package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
