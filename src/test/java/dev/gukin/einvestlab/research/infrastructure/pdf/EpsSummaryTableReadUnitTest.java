package dev.gukin.einvestlab.research.infrastructure.pdf;

import dev.gukin.einvestlab.research.domain.EpsExtraction;
import dev.gukin.einvestlab.research.domain.EpsExtractionStatus;
import dev.gukin.einvestlab.research.domain.EpsFigure;
import dev.gukin.einvestlab.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("EPS 요약표 파싱 단위 테스트 (실물 pdftotext 출력 픽스처)")
class EpsSummaryTableReadUnitTest {

    private final EpsSummaryTableParser parser = new EpsSummaryTableParser();

    @Test
    @DisplayName("세로형: 연도 헤더 열과 EPS 행을 짝지어 음수 포함 4개 연도를 뽑는다")
    void shouldParseVerticalTable() {
        EpsExtraction extraction = parser.parse(fixture("eps-vertical.txt"));

        assertThat(extraction.status()).isEqualTo(EpsExtractionStatus.EXTRACTED);
        assertThat(extraction.figures())
                .extracting(EpsFigure::fiscalYear, EpsFigure::estimated, EpsFigure::eps)
                .containsExactly(
                        tuple(2024, false, new BigDecimal("-1567")),
                        tuple(2025, false, new BigDecimal("-4404")),
                        tuple(2026, true, new BigDecimal("2791")),
                        tuple(2027, true, new BigDecimal("4274"))
                );
    }

    @Test
    @DisplayName("세로형 변형: EPS (원) 라벨과 차트 축 숫자 오염을 이겨내고 5개 연도를 뽑는다")
    void shouldParseVerticalTableWithSidebarNoise() {
        EpsExtraction extraction = parser.parse(fixture("eps-vertical-sidebar.txt"));

        assertThat(extraction.status()).isEqualTo(EpsExtractionStatus.EXTRACTED);
        assertThat(extraction.figures())
                .extracting(EpsFigure::fiscalYear, EpsFigure::estimated, EpsFigure::eps)
                .containsExactly(
                        tuple(2024, false, new BigDecimal("2767")),
                        tuple(2025, false, new BigDecimal("2130")),
                        tuple(2026, true, new BigDecimal("4087")),
                        tuple(2027, true, new BigDecimal("5193")),
                        tuple(2028, true, new BigDecimal("5658"))
                );
    }

    @Test
    @DisplayName("세로형 2단 레이아웃: 본문 산문 오른쪽에 붙은 표에서 라벨 뒤 숫자만 짝짓는다")
    void shouldParseRightColumnTableBesideProse() {
        EpsExtraction extraction = parser.parse(fixture("eps-vertical-right-column.txt"));

        assertThat(extraction.status()).isEqualTo(EpsExtractionStatus.EXTRACTED);
        assertThat(extraction.figures())
                .extracting(EpsFigure::fiscalYear, EpsFigure::estimated, EpsFigure::eps)
                .containsExactly(
                        tuple(2025, false, new BigDecimal("-1806")),
                        tuple(2026, true, new BigDecimal("3109")),
                        tuple(2027, true, new BigDecimal("2765")),
                        tuple(2028, true, new BigDecimal("3957"))
                );
    }

    @Test
    @DisplayName("세로형 연도 헤더에 같은 연도가 두 번 있으면 왼쪽(첫 표) 열만 쓴다")
    void shouldKeepLeftmostColumnOnDuplicateYearHeader() {
        EpsExtraction extraction = parser.parse(fixture("eps-vertical-duplicate-year.txt"));

        assertThat(extraction.status()).isEqualTo(EpsExtractionStatus.EXTRACTED);
        assertThat(extraction.figures())
                .extracting(EpsFigure::fiscalYear, EpsFigure::estimated, EpsFigure::eps)
                .containsExactly(
                        tuple(2024, false, new BigDecimal("12442")),
                        tuple(2025, false, new BigDecimal("14227")),
                        tuple(2026, true, new BigDecimal("14843")),
                        tuple(2027, true, new BigDecimal("14339")),
                        tuple(2028, true, new BigDecimal("15793"))
                );
    }

    @Test
    @DisplayName("세로형 A/F 접미 연도: 후단 페이지 2단 표에서 A는 실적, F는 추정으로 읽는다")
    void shouldParseTwoColumnPageThreeTableWithAnnualSuffixes() {
        EpsExtraction extraction = parser.parse(fixture("eps-vertical-two-column-page3.txt"));

        assertThat(extraction.status()).isEqualTo(EpsExtractionStatus.EXTRACTED);
        assertThat(extraction.figures())
                .extracting(EpsFigure::fiscalYear, EpsFigure::estimated, EpsFigure::eps)
                .containsExactly(
                        tuple(2024, false, new BigDecimal("1780")),
                        tuple(2025, false, new BigDecimal("2706")),
                        tuple(2026, true, new BigDecimal("3224")),
                        tuple(2027, true, new BigDecimal("4298")),
                        tuple(2028, true, new BigDecimal("5607"))
                );
    }

    @Test
    @DisplayName("가로형: 헤더의 EPS 열 위치로 연도 행마다 값을 뽑는다")
    void shouldParseHorizontalTable() {
        EpsExtraction extraction = parser.parse(fixture("eps-horizontal.txt"));

        assertThat(extraction.status()).isEqualTo(EpsExtractionStatus.EXTRACTED);
        assertThat(extraction.figures())
                .extracting(EpsFigure::fiscalYear, EpsFigure::estimated, EpsFigure::eps)
                .containsExactly(
                        tuple(2024, false, new BigDecimal("1722")),
                        tuple(2025, false, new BigDecimal("2656")),
                        tuple(2026, true, new BigDecimal("3851")),
                        tuple(2027, true, new BigDecimal("4891")),
                        tuple(2028, true, new BigDecimal("6336"))
                );
    }

    @Test
    @DisplayName("산문에만 EPS 가 언급되면 요약표 없음으로 판정한다")
    void shouldReportNoSummaryTableForProseOnlyMention() {
        EpsExtraction extraction = parser.parse(fixture("eps-prose-only.txt"));

        assertThat(extraction.status()).isEqualTo(EpsExtractionStatus.NO_SUMMARY_TABLE);
        assertThat(extraction.figures()).isEmpty();
    }

    @Test
    @DisplayName("EPS 라벨 행은 있는데 연도 헤더가 없으면 실패로 판정한다")
    void shouldFailWhenEpsRowHasNoYearHeader() {
        String text = """
                아무 관련 없는 문장
                EPS                1,000        2,000
                또 다른 문장
                """;

        EpsExtraction extraction = parser.parse(text);

        assertThat(extraction.status()).isEqualTo(EpsExtractionStatus.FAILED);
        assertThat(extraction.figures()).isEmpty();
    }

    private String fixture(String name) {
        return Fixtures.read("/fixtures/research/" + name);
    }
}
