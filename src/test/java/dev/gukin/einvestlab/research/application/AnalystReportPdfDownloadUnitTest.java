package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfSource;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfStore;
import dev.gukin.einvestlab.research.domain.ResearchSourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("애널리스트 리포트 PDF 다운로드 유스케이스 단위 테스트")
class AnalystReportPdfDownloadUnitTest {

    private final StubPdfSource pdfSource = new StubPdfSource();
    private final StubPdfStore pdfStore = new StubPdfStore();
    private final StubAnalystReportRepository repository = new StubAnalystReportRepository();
    private final AnalystReportPdfDownloadUseCase useCase =
            new AnalystReportPdfDownloadUseCase(repository, pdfSource, pdfStore);

    @Test
    @DisplayName("PDF 미보유 리포트를 내려받아 저장 경로를 리포트에 남긴다")
    void shouldDownloadAndAttachPath() {
        repository.withoutPdf = List.of(report(1L), report(2L));

        AnalystReportPdfDownloadResult result = useCase.downloadAll();

        assertThat(result).isEqualTo(new AnalystReportPdfDownloadResult(2, 0));
        assertThat(pdfStore.storedReportIdxes).containsExactly(1L, 2L);
        assertThat(repository.saved)
                .extracting(AnalystReport::getPdfPath)
                .containsExactly("2026/08/1.pdf", "2026/08/2.pdf");
    }

    @Test
    @DisplayName("원천 실패는 실패로 세고 나머지는 계속 진행한다")
    void shouldContinueAfterSourceFailure() {
        repository.withoutPdf = List.of(report(1L), report(2L));
        pdfSource.failingReportIdx = 1L;

        AnalystReportPdfDownloadResult result = useCase.downloadAll();

        assertThat(result).isEqualTo(new AnalystReportPdfDownloadResult(1, 1));
        assertThat(repository.saved)
                .extracting(AnalystReport::getReportIdx)
                .containsExactly(2L);
    }

    @Test
    @DisplayName("미보유 리포트가 없으면 아무 것도 내려받지 않는다")
    void shouldDoNothingWithoutPendingReports() {
        AnalystReportPdfDownloadResult result = useCase.downloadAll();

        assertThat(result).isEqualTo(new AnalystReportPdfDownloadResult(0, 0));
        assertThat(pdfSource.fetchedReportIdxes).isEmpty();
        assertThat(repository.saved).isEmpty();
    }

    private AnalystReport report(long reportIdx) {
        return AnalystReport.builder()
                .id(Ids.generate())
                .reportIdx(reportIdx)
                .stockCode("016360")
                .companyName("삼성증권")
                .title("삼성증권(016360) 최대실적 지속 경신")
                .broker("LS증권")
                .publishedDate(LocalDate.of(2026, 8, 5))
                .collectedAt(Instant.parse("2026-08-05T03:00:00Z"))
                .build();
    }

    private static class StubPdfSource implements AnalystReportPdfSource {

        private final List<Long> fetchedReportIdxes = new ArrayList<>();
        private Long failingReportIdx;

        @Override
        public byte[] fetchPdf(long reportIdx) {
            fetchedReportIdxes.add(reportIdx);
            if (failingReportIdx != null && failingReportIdx == reportIdx) {
                throw new ResearchSourceException("PDF 아닌 응답 본문");
            }
            return "%PDF-1.7".getBytes();
        }
    }

    private static class StubPdfStore implements AnalystReportPdfStore {

        private final List<Long> storedReportIdxes = new ArrayList<>();

        @Override
        public String store(long reportIdx, LocalDate publishedDate, byte[] content) {
            storedReportIdxes.add(reportIdx);
            return "%d/%02d/%d.pdf".formatted(
                    publishedDate.getYear(), publishedDate.getMonthValue(), reportIdx);
        }

        @Override
        public Path resolve(String relativePath) {
            return Path.of("/storage-root").resolve(relativePath);
        }
    }

}
