package dev.gukin.einvestlab.research.infrastructure.storage;

import dev.gukin.einvestlab.global.config.PdfStorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("애널리스트 리포트 PDF 파일 저장 단위 테스트")
class AnalystReportPdfStoreUnitTest {

    @TempDir
    Path root;

    @Test
    @DisplayName("발행 연월 디렉터리 아래 report_idx 이름으로 저장하고 상대 경로를 반환한다")
    void shouldStoreUnderPublishedYearMonth() throws Exception {
        AnalystReportPdfStoreAdapter store =
                new AnalystReportPdfStoreAdapter(new PdfStorageProperties(root.toString()));
        byte[] content = "%PDF-1.7 fake".getBytes();

        String relativePath = store.store(12345L, LocalDate.of(2026, 8, 5), content);

        assertThat(relativePath).isEqualTo("2026/08/12345.pdf");
        assertThat(Files.readAllBytes(root.resolve(relativePath))).isEqualTo(content);
    }

    @Test
    @DisplayName("같은 리포트를 다시 저장하면 덮어쓴다")
    void shouldOverwriteOnRestore() throws Exception {
        AnalystReportPdfStoreAdapter store =
                new AnalystReportPdfStoreAdapter(new PdfStorageProperties(root.toString()));
        store.store(12345L, LocalDate.of(2026, 8, 5), "first".getBytes());

        String relativePath = store.store(12345L, LocalDate.of(2026, 8, 5), "second".getBytes());

        assertThat(Files.readAllBytes(root.resolve(relativePath))).isEqualTo("second".getBytes());
    }
}
