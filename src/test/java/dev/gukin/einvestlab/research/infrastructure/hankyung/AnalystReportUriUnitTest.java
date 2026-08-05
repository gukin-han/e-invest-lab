package dev.gukin.einvestlab.research.infrastructure.hankyung;

import dev.gukin.einvestlab.global.config.HankyungApiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("한경 컨센서스 요청 URI 조립 단위 테스트")
class AnalystReportUriUnitTest {

    private final AnalystReportSourceAdapter adapter = new AnalystReportSourceAdapter(
            null,
            new HankyungApiProperties("https://consensus.hankyung.com"),
            null
    );

    @Test
    @DisplayName("기업 분류·기간·페이지 조건으로 목록 URI 를 만든다")
    void shouldBuildListUriWithBusinessFilters() {
        URI uri = adapter.buildListUri(LocalDate.of(2026, 7, 29), LocalDate.of(2026, 8, 5), 3);

        assertThat(uri.toString()).isEqualTo(
                "https://consensus.hankyung.com/analysis/list"
                        + "?skinType=business"
                        + "&sdate=2026-07-29"
                        + "&edate=2026-08-05"
                        + "&now_page=3"
                        + "&pagenum=80");
    }
}
