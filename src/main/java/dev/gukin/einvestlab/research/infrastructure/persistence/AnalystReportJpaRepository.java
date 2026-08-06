package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.EpsExtractionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnalystReportJpaRepository extends JpaRepository<AnalystReport, UUID> {

    boolean existsByReportIdx(long reportIdx);

    List<AnalystReport> findAllByPdfPathIsNull();

    @Query("""
            select r from AnalystReport r
            where r.pdfPath is not null
              and (r.epsExtractionStatus is null or r.epsExtractionStatus = :retryable)
            """)
    List<AnalystReport> findAllPendingEpsExtraction(@Param("retryable") EpsExtractionStatus retryable);
}
