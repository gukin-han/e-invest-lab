package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalystReportJpaRepository extends JpaRepository<AnalystReport, UUID> {

    boolean existsByReportIdx(long reportIdx);

    List<AnalystReport> findAllByPdfPathIsNull();
}
