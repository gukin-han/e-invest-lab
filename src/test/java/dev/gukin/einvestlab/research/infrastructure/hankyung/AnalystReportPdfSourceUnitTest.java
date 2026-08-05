package dev.gukin.einvestlab.research.infrastructure.hankyung;

import dev.gukin.einvestlab.global.config.HankyungApiProperties;
import dev.gukin.einvestlab.research.domain.ResearchSourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("한경 컨센서스 PDF 소스 단위 테스트")
class AnalystReportPdfSourceUnitTest {

    @Test
    @DisplayName("PDF 매직 바이트로 시작하는 본문은 통과시킨다")
    void shouldAcceptPdfBody() {
        byte[] body = "%PDF-1.7 rest of document".getBytes();

        assertThat(AnalystReportPdfSourceAdapter.requirePdf(body, 1L)).isSameAs(body);
    }

    @Test
    @DisplayName("PDF 가 아닌 본문(에러 페이지)은 원천 예외로 거부한다")
    void shouldRejectNonPdfBody() {
        byte[] htmlBody = "<html><body>error</body></html>".getBytes();

        assertThatThrownBy(() -> AnalystReportPdfSourceAdapter.requirePdf(htmlBody, 1L))
                .isInstanceOf(ResearchSourceException.class)
                .hasMessageContaining("report_idx=1");
    }

    @Test
    @DisplayName("탐침으로 확인한 다운로드 URL 형식을 만든다")
    void shouldBuildProbedDownloadUri() {
        AnalystReportPdfSourceAdapter adapter = new AnalystReportPdfSourceAdapter(
                null, new HankyungApiProperties("https://consensus.hankyung.com"));

        assertThat(adapter.buildPdfUri(422780L))
                .hasToString("https://consensus.hankyung.com/analysis/downpdf?report_idx=422780");
    }
}
