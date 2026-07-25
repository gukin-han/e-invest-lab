package dev.gukin.einvestlab.disclosure.infrastructure.dart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("사업의 내용 실물 경계 회귀 스모크 테스트")
class BusinessContentFixtureSmokeTest {

    private final BusinessContentExtractor extractor = new BusinessContentExtractor();

    @Test
    @DisplayName("삼성전자 실물 경계 슬라이스에서 섹션 제목 요소부터 정확히 잘라낸다")
    void shouldExtractFromSamsungBoundaryFixture() {
        String section = extractor.extract(fixture("samsung-boundary.xml"));

        assertThat(section)
                .startsWith("<TITLE ATOC=\"Y\" AASSOCNOTE=\"D-0-2-0-0\" ATOCID=\"9\" ENG=\"II. Business Description\">II. 사업의 내용")
                .contains("1. 사업의 개요")
                .doesNotContain("REFTYPE")
                .doesNotContain("I. 회사의 개요")
                .doesNotContain("재무에 관한 사항");
    }

    @Test
    @DisplayName("카카오뱅크 실물 경계 슬라이스에서 북마크 참조와 제목 속성을 건너뛰고 잘라낸다")
    void shouldExtractFromKakaobankBoundaryFixture() {
        String section = extractor.extract(fixture("kakaobank-boundary.xml"));

        assertThat(section)
                .startsWith("<TITLE ATOC=\"Y\" AASSOCNOTE=\"D-0-2-0-0\" ATOCID=\"9\" ID=\"II. 사업의 내용\" ENG=\"II. Business Description\">II. 사업의 내용")
                .contains("1. 사업의 개요")
                .doesNotContain("REFTYPE")
                .doesNotContain("I. 회사의 개요")
                .doesNotContain("재무에 관한 사항");
    }

    private String fixture(String name) {
        try (InputStream input = getClass().getResourceAsStream("/fixtures/disclosure/" + name)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
