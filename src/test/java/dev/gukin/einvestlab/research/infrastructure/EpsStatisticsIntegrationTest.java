package dev.gukin.einvestlab.research.infrastructure;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.EpsConsensus;
import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.domain.EpsRevision;
import dev.gukin.einvestlab.research.infrastructure.persistence.AnalystReportJpaRepository;
import dev.gukin.einvestlab.research.infrastructure.persistence.EpsEstimateJpaRepository;
import dev.gukin.einvestlab.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("EPS 통계 조회 통합 테스트")
class EpsStatisticsIntegrationTest extends AbstractIntegrationTest {

    private static final String STOCK = "016360";
    private static final String OTHER_STOCK = "000001";
    private static final LocalDate SINCE = LocalDate.of(2026, 2, 7);

    @Autowired
    private EpsEstimateRepository estimateRepository;

    @Autowired
    private AnalystReportJpaRepository reportJpa;

    @Autowired
    private EpsEstimateJpaRepository estimateJpa;

    @BeforeEach
    void seed() {
        report(1L, STOCK, "LS증권", LocalDate.of(2026, 7, 1), "5000");
        report(2L, STOCK, "LS증권", LocalDate.of(2026, 8, 1), "5500");
        report(3L, STOCK, "IBK투자증권", LocalDate.of(2026, 8, 1), "6500");
        report(4L, STOCK, "대신증권", LocalDate.of(2026, 8, 3), "6000");
        report(5L, STOCK, "대신증권", LocalDate.of(2026, 8, 3), "6000");
        report(6L, STOCK, "유진투자증권", LocalDate.of(2026, 1, 5), "9999");
        report(7L, OTHER_STOCK, "LS증권", LocalDate.of(2026, 8, 1), "7777");
    }

    @AfterEach
    void tearDown() {
        estimateJpa.deleteAllInBatch();
        reportJpa.deleteAllInBatch();
    }

    @Nested
    @DisplayName("컨센서스를 조회할 때")
    class WhenFindingConsensus {

        @Test
        @DisplayName("증권사별 최신 추정치만 평균에 넣고, 중복 게시는 한 번만, 유효기간 밖은 뺀다")
        void shouldAverageLatestPerBrokerWithinWindow() {
            assertThat(estimateRepository.findConsensus(STOCK, SINCE))
                    .extracting(EpsConsensus::fiscalYear, EpsConsensus::sampleCount,
                            consensus -> consensus.averageEps().stripTrailingZeros(),
                            consensus -> consensus.minEps().stripTrailingZeros(),
                            consensus -> consensus.maxEps().stripTrailingZeros())
                    .containsExactly(tuple(2026, 3L,
                            new BigDecimal("6000").stripTrailingZeros(),
                            new BigDecimal("5500").stripTrailingZeros(),
                            new BigDecimal("6500").stripTrailingZeros()));
        }

        @Test
        @DisplayName("다른 종목의 추정치는 섞이지 않는다")
        void shouldIsolateByStock() {
            assertThat(estimateRepository.findConsensus(OTHER_STOCK, SINCE))
                    .extracting(EpsConsensus::sampleCount,
                            consensus -> consensus.averageEps().stripTrailingZeros())
                    .containsExactly(tuple(1L, new BigDecimal("7777").stripTrailingZeros()));
        }
    }

    @Nested
    @DisplayName("최근 커버리지 종목을 조회할 때")
    class WhenFindingRecentlyCovered {

        @Test
        @DisplayName("종목별 리포트·증권사 수를 세되 중복 게시는 한 건으로, 기간 밖은 뺀다")
        void shouldAggregateCoveragePerStock() {
            assertThat(reportJpa.findRecentlyCovered(SINCE))
                    .extracting(AnalystReportJpaRepository.CoveredStockRow::getStockCode,
                            AnalystReportJpaRepository.CoveredStockRow::getReportCount,
                            AnalystReportJpaRepository.CoveredStockRow::getBrokerCount,
                            AnalystReportJpaRepository.CoveredStockRow::getLatestPublishedDate)
                    .containsExactly(
                            tuple(STOCK, 4L, 3L, LocalDate.of(2026, 8, 3)),
                            tuple(OTHER_STOCK, 1L, 1L, LocalDate.of(2026, 8, 1))
                    );
        }
    }

    @Nested
    @DisplayName("리비전 추이를 조회할 때")
    class WhenFindingRevisions {

        @Test
        @DisplayName("전체 이력을 발행일 순으로 주되, 같은 리포트의 중복 게시는 한 행으로 합친다")
        void shouldListFullHistoryInPublishedOrder() {
            assertThat(estimateRepository.findRevisions(STOCK, 2026))
                    .extracting(EpsRevision::publishedDate, EpsRevision::broker,
                            revision -> revision.eps().stripTrailingZeros())
                    .containsExactly(
                            tuple(LocalDate.of(2026, 1, 5), "유진투자증권",
                                    new BigDecimal("9999").stripTrailingZeros()),
                            tuple(LocalDate.of(2026, 7, 1), "LS증권",
                                    new BigDecimal("5000").stripTrailingZeros()),
                            tuple(LocalDate.of(2026, 8, 1), "IBK투자증권",
                                    new BigDecimal("6500").stripTrailingZeros()),
                            tuple(LocalDate.of(2026, 8, 1), "LS증권",
                                    new BigDecimal("5500").stripTrailingZeros()),
                            tuple(LocalDate.of(2026, 8, 3), "대신증권",
                                    new BigDecimal("6000").stripTrailingZeros())
                    );
        }
    }

    private void report(long reportIdx, String stockCode, String broker,
                        LocalDate publishedDate, String eps2026) {
        reportJpa.save(AnalystReport.builder()
                .id(Ids.generate())
                .reportIdx(reportIdx)
                .stockCode(stockCode)
                .companyName("테스트종목")
                .title("테스트종목(%s) 리포트".formatted(stockCode))
                .broker(broker)
                .publishedDate(publishedDate)
                .collectedAt(Instant.parse("2026-08-07T03:00:00Z"))
                .build());
        estimateJpa.save(EpsEstimate.builder()
                .id(Ids.generate())
                .reportIdx(reportIdx)
                .fiscalYear(2026)
                .estimated(true)
                .eps(new BigDecimal(eps2026))
                .extractedAt(Instant.parse("2026-08-07T03:00:00Z"))
                .build());
    }
}
