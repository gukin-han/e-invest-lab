package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfStore;
import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.domain.EpsExtraction;
import dev.gukin.einvestlab.research.domain.EpsExtractionStatus;
import dev.gukin.einvestlab.research.domain.EpsExtractor;
import dev.gukin.einvestlab.research.domain.EpsFigure;
import dev.gukin.einvestlab.research.domain.PdfTextExtractionException;
import dev.gukin.einvestlab.support.RecordingTransactionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("애널리스트 리포트 EPS 추출 유스케이스 단위 테스트")
class AnalystReportEpsExtractUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-08-07T03:00:00Z");

    private final StubAnalystReportRepository reportRepository = new StubAnalystReportRepository();
    private final StubEstimateRepository estimateRepository = new StubEstimateRepository();
    private final StubExtractor extractor = new StubExtractor();
    private final AnalystReportEpsExtractUseCase useCase = new AnalystReportEpsExtractUseCase(
            reportRepository, estimateRepository, new StubPdfStore(), extractor,
            new RecordingTransactionManager());

    @Test
    @DisplayName("추출 성공이면 추정치를 저장하고 리포트를 EXTRACTED 로 기록한다")
    void shouldSaveEstimatesAndMarkExtracted() {
        reportRepository.pendingEpsExtraction = List.of(report(1L));
        extractor.results.put(1L, EpsExtraction.extracted(List.of(
                new EpsFigure(2025, false, new BigDecimal("2130")),
                new EpsFigure(2026, true, new BigDecimal("4087")))));

        AnalystReportEpsExtractResult result = useCase.extractAll(BASE_TIME);

        assertThat(result).isEqualTo(new AnalystReportEpsExtractResult(1, 0, 0));
        assertThat(estimateRepository.saved)
                .extracting(EpsEstimate::getReportIdx, EpsEstimate::getFiscalYear,
                        EpsEstimate::isEstimated, EpsEstimate::getEps, EpsEstimate::getExtractedAt)
                .containsExactly(
                        tuple(1L, 2025, false, new BigDecimal("2130"), BASE_TIME),
                        tuple(1L, 2026, true, new BigDecimal("4087"), BASE_TIME)
                );
        assertThat(estimateRepository.saved).extracting(EpsEstimate::getId).doesNotContainNull();
        assertThat(reportRepository.saved.getFirst().getEpsExtractionStatus())
                .isEqualTo(EpsExtractionStatus.EXTRACTED);
    }

    @Test
    @DisplayName("요약표 없음이면 추정치 없이 상태만 확정 기록한다")
    void shouldMarkNoSummaryTableWithoutEstimates() {
        reportRepository.pendingEpsExtraction = List.of(report(1L));
        extractor.results.put(1L, EpsExtraction.noSummaryTable());

        AnalystReportEpsExtractResult result = useCase.extractAll(BASE_TIME);

        assertThat(result).isEqualTo(new AnalystReportEpsExtractResult(0, 1, 0));
        assertThat(estimateRepository.saved).isEmpty();
        assertThat(reportRepository.saved.getFirst().getEpsExtractionStatus())
                .isEqualTo(EpsExtractionStatus.NO_SUMMARY_TABLE);
    }

    @Test
    @DisplayName("PDF 처리 실패는 실패로 세고 나머지 리포트는 계속 진행한다")
    void shouldContinueAfterExtractionFailure() {
        reportRepository.pendingEpsExtraction = List.of(report(1L), report(2L));
        extractor.failingReportIdx = 1L;
        extractor.results.put(2L, EpsExtraction.extracted(List.of(
                new EpsFigure(2026, true, new BigDecimal("1000")),
                new EpsFigure(2027, true, new BigDecimal("1100")))));

        AnalystReportEpsExtractResult result = useCase.extractAll(BASE_TIME);

        assertThat(result).isEqualTo(new AnalystReportEpsExtractResult(1, 0, 1));
        assertThat(reportRepository.saved)
                .extracting(AnalystReport::getReportIdx, AnalystReport::getEpsExtractionStatus)
                .containsExactly(
                        tuple(1L, EpsExtractionStatus.FAILED),
                        tuple(2L, EpsExtractionStatus.EXTRACTED)
                );
    }

    @Test
    @DisplayName("발행연도에서 먼 연도가 섞이면 저장하지 않고 실패로 기록한다")
    void shouldFailOnOutOfRangeFiscalYear() {
        reportRepository.pendingEpsExtraction = List.of(report(1L));
        extractor.results.put(1L, EpsExtraction.extracted(List.of(
                new EpsFigure(2026, false, new BigDecimal("1000")),
                new EpsFigure(2090, true, new BigDecimal("9999")))));

        AnalystReportEpsExtractResult result = useCase.extractAll(BASE_TIME);

        assertThat(result).isEqualTo(new AnalystReportEpsExtractResult(0, 0, 1));
        assertThat(estimateRepository.saved).isEmpty();
        assertThat(reportRepository.saved.getFirst().getEpsExtractionStatus())
                .isEqualTo(EpsExtractionStatus.FAILED);
    }

    @Test
    @DisplayName("같은 연도가 두 번 나오면 저장하지 않고 실패로 기록한다")
    void shouldFailOnDuplicateFiscalYear() {
        reportRepository.pendingEpsExtraction = List.of(report(1L));
        extractor.results.put(1L, EpsExtraction.extracted(List.of(
                new EpsFigure(2026, true, new BigDecimal("1000")),
                new EpsFigure(2026, true, new BigDecimal("2000")))));

        AnalystReportEpsExtractResult result = useCase.extractAll(BASE_TIME);

        assertThat(result).isEqualTo(new AnalystReportEpsExtractResult(0, 0, 1));
        assertThat(estimateRepository.saved).isEmpty();
    }

    private AnalystReport report(long reportIdx) {
        AnalystReport report = AnalystReport.builder()
                .id(Ids.generate())
                .reportIdx(reportIdx)
                .stockCode("016360")
                .companyName("삼성증권")
                .title("삼성증권(016360) 최대실적 지속 경신")
                .broker("LS증권")
                .publishedDate(LocalDate.of(2026, 8, 5))
                .collectedAt(BASE_TIME)
                .build();
        report.attachPdf("2026/08/" + reportIdx + ".pdf");
        return report;
    }

    private static class StubExtractor implements EpsExtractor {

        private final Map<Long, EpsExtraction> results = new HashMap<>();
        private Long failingReportIdx;

        @Override
        public EpsExtraction extract(Path pdfFile) {
            long reportIdx = reportIdxFrom(pdfFile);
            if (failingReportIdx != null && failingReportIdx == reportIdx) {
                throw new PdfTextExtractionException("pdftotext 실행 실패");
            }
            return results.get(reportIdx);
        }

        private long reportIdxFrom(Path pdfFile) {
            String name = pdfFile.getFileName().toString();
            return Long.parseLong(name.substring(0, name.length() - ".pdf".length()));
        }
    }

    private static class StubPdfStore implements AnalystReportPdfStore {

        @Override
        public String store(long reportIdx, LocalDate publishedDate, byte[] content) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path resolve(String relativePath) {
            return Path.of("/storage-root").resolve(relativePath);
        }
    }

    private static class StubEstimateRepository implements EpsEstimateRepository {

        private final List<EpsEstimate> saved = new ArrayList<>();

        @Override
        public void saveAll(List<EpsEstimate> estimates) {
            saved.addAll(estimates);
        }
    }

}
