package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("애널리스트 리포트 PDF 정리(롤링 1년) 단위 테스트")
class AnalystReportPdfPurgeUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-08-17T09:00:00Z");

    private final StubAnalystReportRepository repository = new StubAnalystReportRepository();
    private final RecordingPdfStore pdfStore = new RecordingPdfStore();
    private final AnalystReportPdfPurgeUseCase useCase = new AnalystReportPdfPurgeUseCase(repository, pdfStore);

    @Test
    @DisplayName("기준 시각의 한국 날짜에서 1년 전을 컷오프로 조회하고, 파일 삭제 후 purged 로 마킹한다")
    void shouldDeleteFileThenMarkPurged() {
        AnalystReport old = reportWithPdf(1L, LocalDate.of(2025, 6, 1));
        repository.withPdfPublishedBefore = List.of(old);

        AnalystReportPdfPurgeResult result = useCase.purgeAll(BASE_TIME);

        assertThat(repository.requestedPurgeCutoff).isEqualTo(LocalDate.of(2025, 8, 17));
        assertThat(result).isEqualTo(new AnalystReportPdfPurgeResult(1, 0));
        assertThat(pdfStore.deleted).containsExactly("2025/06/1.pdf");
        assertThat(old.getPdfPath()).isNull();
        assertThat(old.getPdfPurgedAt()).isEqualTo(BASE_TIME);
        assertThat(repository.saved).containsExactly(old);
    }

    @Test
    @DisplayName("파일 삭제가 실패하면 마킹하지 않고 실패로 세어 다음 실행이 다시 시도하게 둔다")
    void shouldNotMarkWhenDeleteFails() {
        AnalystReport failing = reportWithPdf(1L, LocalDate.of(2025, 6, 1));
        AnalystReport healthy = reportWithPdf(2L, LocalDate.of(2025, 7, 1));
        repository.withPdfPublishedBefore = List.of(failing, healthy);
        pdfStore.failingPath = "2025/06/1.pdf";

        AnalystReportPdfPurgeResult result = useCase.purgeAll(BASE_TIME);

        assertThat(result).isEqualTo(new AnalystReportPdfPurgeResult(1, 1));
        assertThat(failing.getPdfPath()).isEqualTo("2025/06/1.pdf");
        assertThat(failing.getPdfPurgedAt()).isNull();
        assertThat(repository.saved).containsExactly(healthy);
    }

    private static AnalystReport reportWithPdf(long reportIdx, LocalDate publishedDate) {
        AnalystReport report = AnalystReport.builder()
                .id(Ids.generate()).reportIdx(reportIdx).stockCode("016360").companyName("삼성증권")
                .title("삼성증권(016360)").broker("LS증권").publishedDate(publishedDate)
                .collectedAt(BASE_TIME).build();
        report.attachPdf("%d/%02d/%d.pdf".formatted(publishedDate.getYear(), publishedDate.getMonthValue(), reportIdx));
        return report;
    }

    private static class RecordingPdfStore implements AnalystReportPdfStore {
        final List<String> deleted = new ArrayList<>();
        String failingPath;

        @Override
        public String store(long reportIdx, LocalDate publishedDate, byte[] content) {
            return null;
        }

        @Override
        public Path resolve(String relativePath) {
            return Path.of(relativePath);
        }

        @Override
        public boolean exists(String relativePath) {
            return true;
        }

        @Override
        public void delete(String relativePath) {
            if (relativePath.equals(failingPath)) {
                throw new UncheckedIOException(new IOException("permission denied"));
            }
            deleted.add(relativePath);
        }
    }
}
