package dev.gukin.einvestlab.disclosure.infrastructure.dart;

import dev.gukin.einvestlab.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    @Test
    @DisplayName("실물 슬라이스에서 추출 → 슬라이스 연쇄가 태그 없는 선별 텍스트를 만든다")
    void shouldChainExtractAndSliceOnRealFixture() {
        BusinessContentSlicer slicer = new BusinessContentSlicer();

        String sliced = slicer.slice(extractor.extract(fixture("samsung-boundary.xml")));

        assertThat(sliced)
                .contains("1. 사업의 개요")
                .contains("글로벌 전자 기업")
                .doesNotContain("<TITLE")
                .doesNotContain("<SECTION");
    }

    private String fixture(String name) {
        return Fixtures.read("/fixtures/disclosure/" + name);
    }
}
