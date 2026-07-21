package dev.gukin.einvestlab.disclosure.infra.dart;

import dev.gukin.einvestlab.global.config.DartApiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DART 요청 URI 조립 단위 테스트")
class BusinessReportUriUnitTest {

    private final BusinessReportAdapter adapter = new BusinessReportAdapter(
            null,
            new DartApiProperties("https://opendart.fss.or.kr/api", "test-key"),
            null,
            null,
            null
    );

    @Test
    @DisplayName("기준 시각을 한국 날짜로 해석해 사업보고서·최종보고서·2년 범위 공시검색 URI 를 만든다")
    void shouldBuildListUriWithAnnualReportFilters() {
        URI uri = adapter.buildListUri("00126380", Instant.parse("2026-07-19T03:00:00Z"));

        assertThat(uri.toString()).isEqualTo(
                "https://opendart.fss.or.kr/api/list.json"
                        + "?crtfc_key=test-key"
                        + "&corp_code=00126380"
                        + "&pblntf_detail_ty=A001"
                        + "&bgn_de=20240719"
                        + "&end_de=20260719"
                        + "&page_count=100");
    }

    @Test
    @DisplayName("한국 자정 직후의 기준 시각은 UTC 로는 전날이어도 한국 날짜를 쓴다")
    void shouldInterpretBaseTimeInKoreanDate() {
        URI uri = adapter.buildListUri("00126380", Instant.parse("2026-07-18T15:30:00Z"));

        assertThat(uri.toString())
                .contains("bgn_de=20240719")
                .contains("end_de=20260719");
    }

    @Test
    @DisplayName("접수번호로 원문 다운로드 URI 를 만든다")
    void shouldBuildDocumentUri() {
        URI uri = adapter.buildDocumentUri("20260310002820");

        assertThat(uri.toString()).isEqualTo(
                "https://opendart.fss.or.kr/api/document.xml"
                        + "?crtfc_key=test-key"
                        + "&rcept_no=20260310002820");
    }
}
