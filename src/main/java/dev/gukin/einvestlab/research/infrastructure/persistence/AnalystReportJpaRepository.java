package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.EpsExtractionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    interface CoveredStockRow {
        String getStockCode();

        String getCompanyName();

        Long getReportCount();

        Long getBrokerCount();

        LocalDate getLatestPublishedDate();
    }

    @Query(nativeQuery = true, value = """
            SELECT r.stock_code                                        AS stockCode,
                   MAX(r.company_name)                                 AS companyName,
                   COUNT(DISTINCT r.broker, r.published_date, r.title) AS reportCount,
                   COUNT(DISTINCT r.broker)                            AS brokerCount,
                   MAX(r.published_date)                               AS latestPublishedDate
            FROM analyst_reports r
            WHERE r.published_date >= :since
            GROUP BY r.stock_code
            ORDER BY latestPublishedDate DESC, reportCount DESC
            """)
    List<CoveredStockRow> findRecentlyCovered(@Param("since") LocalDate since);
}
