package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.EpsEstimate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EpsEstimateJpaRepository extends JpaRepository<EpsEstimate, UUID> {

    void deleteAllByReportIdx(long reportIdx);

    interface ConsensusRow {
        Integer getFiscalYear();

        BigDecimal getAverageEps();

        Long getSampleCount();

        BigDecimal getMinEps();

        BigDecimal getMaxEps();
    }

    interface RevisionRow {
        LocalDate getPublishedDate();

        String getBroker();

        BigDecimal getEps();

        Boolean getEstimated();
    }

    @Query(nativeQuery = true, value = """
            SELECT t.fiscal_year        AS fiscalYear,
                   AVG(t.broker_eps)    AS averageEps,
                   COUNT(*)             AS sampleCount,
                   MIN(t.broker_eps)    AS minEps,
                   MAX(t.broker_eps)    AS maxEps
            FROM (
                SELECT e.fiscal_year, r.broker, AVG(e.eps) AS broker_eps
                FROM eps_estimates e
                JOIN analyst_reports r ON r.report_idx = e.report_idx
                JOIN (
                    SELECT r2.broker AS broker, e2.fiscal_year AS fiscal_year,
                           MAX(r2.published_date) AS latest_date
                    FROM eps_estimates e2
                    JOIN analyst_reports r2 ON r2.report_idx = e2.report_idx
                    WHERE r2.stock_code = :stockCode AND r2.published_date >= :since
                    GROUP BY r2.broker, e2.fiscal_year
                ) latest ON latest.broker = r.broker
                        AND latest.fiscal_year = e.fiscal_year
                        AND latest.latest_date = r.published_date
                WHERE r.stock_code = :stockCode AND r.published_date >= :since
                GROUP BY e.fiscal_year, r.broker
            ) t
            GROUP BY t.fiscal_year
            ORDER BY t.fiscal_year
            """)
    List<ConsensusRow> findConsensus(@Param("stockCode") String stockCode, @Param("since") LocalDate since);

    @Query(nativeQuery = true, value = """
            SELECT DISTINCT r.published_date AS publishedDate,
                            r.broker         AS broker,
                            e.eps            AS eps,
                            e.estimated      AS estimated
            FROM eps_estimates e
            JOIN analyst_reports r ON r.report_idx = e.report_idx
            WHERE r.stock_code = :stockCode AND e.fiscal_year = :fiscalYear
            ORDER BY publishedDate, broker
            """)
    List<RevisionRow> findRevisions(@Param("stockCode") String stockCode, @Param("fiscalYear") int fiscalYear);
}
