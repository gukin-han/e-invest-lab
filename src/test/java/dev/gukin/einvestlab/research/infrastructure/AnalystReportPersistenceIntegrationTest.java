package dev.gukin.einvestlab.research.infrastructure;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.EpsExtractionStatus;
import dev.gukin.einvestlab.research.infrastructure.persistence.AnalystReportJpaRepository;
import dev.gukin.einvestlab.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("애널리스트 리포트 영속화 통합 테스트")
class AnalystReportPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AnalystReportJpaRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("리포트를 저장하고 다시 읽을 때")
    class WhenSavingAndReading {

        @Test
        @DisplayName("목록 메타와 수집 시각이 그대로 보존된다")
        void shouldRoundTripReport() {
            UUID id = Ids.generate();
            Instant collectedAt = Instant.parse("2026-08-05T03:00:00Z");

            repository.save(AnalystReport.builder()
                    .id(id)
                    .reportIdx(651490L)
                    .stockCode("016360")
                    .companyName("삼성증권")
                    .title("삼성증권(016360) 최대실적 지속 경신")
                    .broker("LS증권")
                    .authors("전배승")
                    .publishedDate(LocalDate.of(2026, 8, 5))
                    .targetPrice(115_000L)
                    .opinion("Hold")
                    .collectedAt(collectedAt)
                    .build());

            AnalystReport found = repository.findById(id).orElseThrow();
            assertThat(found.getReportIdx()).isEqualTo(651490L);
            assertThat(found.getTargetPrice()).isEqualTo(115_000L);
            assertThat(found.getCollectedAt()).isEqualTo(collectedAt);
            assertThat(repository.existsByReportIdx(651490L)).isTrue();
        }
    }

    @Nested
    @DisplayName("EPS 추출 대상을 조회할 때")
    class WhenFindingPendingEpsExtraction {

        @Test
        @DisplayName("PDF 보유 + 미시도/실패만 나오고, 확정 상태는 빠진다")
        void shouldFindOnlyPendingReports() {
            AnalystReport withoutPdf = saved(1L, null);
            AnalystReport untried = saved(2L, null);
            untried.attachPdf("2026/08/2.pdf");
            AnalystReport failed = saved(3L, EpsExtractionStatus.FAILED);
            AnalystReport extracted = saved(4L, EpsExtractionStatus.EXTRACTED);
            AnalystReport noTable = saved(5L, EpsExtractionStatus.NO_SUMMARY_TABLE);
            repository.saveAll(java.util.List.of(withoutPdf, untried, failed, extracted, noTable));

            assertThat(repository.findAllPendingEpsExtraction(EpsExtractionStatus.FAILED))
                    .extracting(AnalystReport::getReportIdx)
                    .containsExactlyInAnyOrder(2L, 3L);
        }

        private AnalystReport saved(long reportIdx, EpsExtractionStatus status) {
            AnalystReport report = AnalystReport.builder()
                    .id(Ids.generate())
                    .reportIdx(reportIdx)
                    .stockCode("016360")
                    .companyName("삼성증권")
                    .title("삼성증권(016360) 최대실적 지속 경신")
                    .broker("LS증권")
                    .publishedDate(LocalDate.of(2026, 8, 5))
                    .collectedAt(Instant.parse("2026-08-05T03:00:00Z"))
                    .build();
            if (status != null) {
                report.attachPdf("2026/08/" + reportIdx + ".pdf");
                report.recordEpsExtraction(status);
            }
            return report;
        }
    }

    @Nested
    @DisplayName("PDF 경로를 붙일 때")
    class WhenAttachingPdfPath {

        @Test
        @DisplayName("경로가 없는 리포트만 미보유로 조회되고, 붙이면 빠진다")
        void shouldFindOnlyReportsWithoutPdfPath() {
            UUID id = Ids.generate();
            repository.save(sample(id, 651490L));

            assertThat(repository.findAllByPdfPathIsNull())
                    .extracting(AnalystReport::getReportIdx)
                    .containsExactly(651490L);

            AnalystReport report = repository.findById(id).orElseThrow();
            report.attachPdf("2026/08/651490.pdf");
            repository.saveAndFlush(report);

            assertThat(repository.findAllByPdfPathIsNull()).isEmpty();
            assertThat(repository.findById(id).orElseThrow().getPdfPath())
                    .isEqualTo("2026/08/651490.pdf");
        }

        private AnalystReport sample(UUID id, long reportIdx) {
            return AnalystReport.builder()
                    .id(id)
                    .reportIdx(reportIdx)
                    .stockCode("016360")
                    .companyName("삼성증권")
                    .title("삼성증권(016360) 최대실적 지속 경신")
                    .broker("LS증권")
                    .publishedDate(LocalDate.of(2026, 8, 5))
                    .collectedAt(Instant.parse("2026-08-05T03:00:00Z"))
                    .build();
        }
    }

    @Nested
    @DisplayName("같은 리포트 식별자로 두 번 저장할 때")
    class WhenSavingDuplicateReportIdx {

        @Test
        @DisplayName("두 번째 저장은 중복으로 거부된다")
        void shouldRejectDuplicateReportIdx() {
            saveSample(651490L);

            assertThatThrownBy(() -> saveSample(651490L))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        private void saveSample(long reportIdx) {
            repository.saveAndFlush(AnalystReport.builder()
                    .id(Ids.generate())
                    .reportIdx(reportIdx)
                    .stockCode("016360")
                    .companyName("삼성증권")
                    .title("삼성증권(016360) 최대실적 지속 경신")
                    .broker("LS증권")
                    .publishedDate(LocalDate.of(2026, 8, 5))
                    .collectedAt(Instant.parse("2026-08-05T03:00:00Z"))
                    .build());
        }
    }
}
