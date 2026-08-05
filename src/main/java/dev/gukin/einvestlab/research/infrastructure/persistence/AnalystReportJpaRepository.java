package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalystReportJpaRepository extends JpaRepository<AnalystReport, UUID> {

    boolean existsByReportIdx(long reportIdx);
}
