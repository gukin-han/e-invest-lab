package dev.gukin.einvestlab.disclosure.infrastructure.dart;

import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("사업의 내용 섹션 추출 단위 테스트")
class BusinessContentExtractUnitTest {

    private static final String FILLER = "<P>" + "사업 내용 본문 ".repeat(200) + "</P>";

    private final BusinessContentExtractor extractor = new BusinessContentExtractor();

    @Test
    @DisplayName("II. 사업의 내용 제목 요소부터 III. 제목 요소 직전까지 하위 항목을 포함해 잘라낸다")
    void shouldExtractBusinessContentSection() {
        String document = """
                <TITLE ATOC="Y" ENG="I. Company overview">I. 회사의 개요</TITLE>
                회사 개요 내용
                <TITLE ATOC="Y" ENG="II. Business contents">II. 사업의 내용</TITLE>
                <TITLE ATOC="Y">1. 사업의 개요</TITLE>
                %s
                <TITLE ATOC="Y">2. 주요 제품 및 서비스</TITLE>
                <TABLE>부문별 매출표</TABLE>
                <TITLE ATOC="Y" ENG="III. Financial affairs">III. 재무에 관한 사항</TITLE>
                재무 내용
                """.formatted(FILLER);

        String section = extractor.extract(document);

        assertThat(section)
                .startsWith("<TITLE ATOC=\"Y\" ENG=\"II. Business contents\">II. 사업의 내용")
                .contains("1. 사업의 개요")
                .contains("<TABLE>부문별 매출표</TABLE>")
                .doesNotContain("재무에 관한 사항")
                .doesNotContain("회사 개요 내용");
    }

    @Test
    @DisplayName("앞쪽 본문의 북마크 참조 속성은 시작 경계로 보지 않는다")
    void shouldIgnoreBookmarkReferenceBeforeSection() {
        String document = """
                <TITLE ATOC="Y">I. 회사의 개요</TITLE>
                <P>사업에 대한 자세한 사항은 <A ATITLE="II. 사업의 내용" REFTYPE="BOOKMARK">여기</A>를 참고.</P>
                I장 본문
                <TITLE ATOC="Y">II. 사업의 내용</TITLE>
                <TITLE ATOC="Y">1. 사업의 개요</TITLE>
                %s
                <TITLE ATOC="Y" ENG="III. Financial affairs">III. 재무에 관한 사항</TITLE>
                """.formatted(FILLER);

        String section = extractor.extract(document);

        assertThat(section)
                .startsWith("<TITLE ATOC=\"Y\">II. 사업의 내용")
                .contains("사업 내용 본문")
                .doesNotContain("I장 본문")
                .doesNotContain("REFTYPE");
    }

    @Test
    @DisplayName("제목 안 공백 변형(연속 공백·줄바꿈)을 허용한다")
    void shouldTolerateWhitespaceVariants() {
        String body = "<TITLE>1. 개요</TITLE> " + "본문 텍스트 ".repeat(200);
        String document = "머리말 <TITLE ATOC=\"Y\">II.  사업의\n내용</TITLE> " + body + "<TITLE>III. 재무</TITLE>";

        String section = extractor.extract(document);

        assertThat(section).isEqualTo("<TITLE ATOC=\"Y\">II.  사업의\n내용</TITLE> " + body);
    }

    @Test
    @DisplayName("시작 경계가 없으면 예외를 던진다")
    void shouldThrowWhenStartBoundaryMissing() {
        assertThatThrownBy(() -> extractor.extract("<TITLE>I. 회사의 개요</TITLE> <TITLE>III. 재무</TITLE>"))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("시작 경계");
    }

    @Test
    @DisplayName("끝 경계가 없으면 예외를 던진다")
    void shouldThrowWhenEndBoundaryMissing() {
        assertThatThrownBy(() -> extractor.extract("<TITLE>II. 사업의 내용</TITLE> 본문이 여기서 끝남"))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("끝 경계");
    }

    @Test
    @DisplayName("섹션이 비정상적으로 짧으면 저장 전에 예외를 던진다")
    void shouldThrowWhenSectionTooShort() {
        String document = "<TITLE>II. 사업의 내용</TITLE> <TITLE>1. 개요</TITLE> 한 줄 <TITLE>III. 재무</TITLE>";

        assertThatThrownBy(() -> extractor.extract(document))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("짧음");
    }

    @Test
    @DisplayName("섹션이 비정상적으로 길면 저장 전에 예외를 던진다")
    void shouldThrowWhenSectionTooLong() {
        String document = "<TITLE>II. 사업의 내용</TITLE> <TITLE>1. 개요</TITLE> "
                + "가".repeat(5_000_001)
                + " <TITLE>III. 재무</TITLE>";

        assertThatThrownBy(() -> extractor.extract(document))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("긺");
    }

    @Test
    @DisplayName("하위 항목 제목이 하나도 없으면 예외를 던진다")
    void shouldThrowWhenNoSubsectionTitle() {
        String document = "<TITLE>II. 사업의 내용</TITLE> " + "본문 텍스트 ".repeat(200) + "<TITLE>III. 재무</TITLE>";

        assertThatThrownBy(() -> extractor.extract(document))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("하위 항목");
    }
}
