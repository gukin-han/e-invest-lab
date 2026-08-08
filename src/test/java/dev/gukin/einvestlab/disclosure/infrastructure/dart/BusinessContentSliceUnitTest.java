package dev.gukin.einvestlab.disclosure.infrastructure.dart;

import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("사업의 내용 슬라이스 단위 테스트")
class BusinessContentSliceUnitTest {

    private final BusinessContentSlicerAdapter slicer = new BusinessContentSlicerAdapter();

    @Test
    @DisplayName("일반 서식은 사업의 개요·주요 제품·매출만 선별하고 나머지는 버린다")
    void shouldSliceGeneralSections() {
        String content = """
                <TITLE ATOC="Y">II. 사업의 내용</TITLE>
                <TITLE ATOC="Y">1. 사업의 개요</TITLE>
                <P>글로벌 전자 기업입니다.</P>
                <TITLE ATOC="Y">2. 주요 제품 및 서비스</TITLE>
                <TABLE><TR><TD>DX 부문</TD><TD>TV, 모니터</TD><TD>1,879,673</TD><TD>56.3%</TD></TR></TABLE>
                <TITLE ATOC="Y">3. 원재료 및 생산설비</TITLE>
                <P>웨이퍼 매입 현황</P>
                <TITLE ATOC="Y">4. 매출 및 수주상황</TITLE>
                <P>부문별 매출실적</P>
                <TITLE ATOC="Y">5. 위험관리 및 파생거래</TITLE>
                <P>환헤지 내용</P>
                <TITLE ATOC="Y">6. 주요계약 및 연구개발활동</TITLE>
                <P>연구개발비</P>
                <TITLE ATOC="Y">7. 기타 참고사항</TITLE>
                <P>상표 현황</P>
                """;

        String sliced = slicer.slice(content);

        assertThat(sliced)
                .contains("1. 사업의 개요")
                .contains("글로벌 전자 기업입니다.")
                .contains("2. 주요 제품 및 서비스")
                .contains("4. 매출 및 수주상황")
                .doesNotContain("웨이퍼 매입 현황")
                .doesNotContain("환헤지 내용")
                .doesNotContain("연구개발비")
                .doesNotContain("상표 현황");
    }

    @Test
    @DisplayName("표는 행 구조를 보존해 변환하고 엔티티를 되돌린다")
    void shouldConvertTablesPreservingRows() {
        String content = """
                <TITLE ATOC="Y">1. 사업의 개요</TITLE>
                <P>R&amp;D 중심 기업</P>
                <TABLE><TR><TD>DX 부문</TD><TD>TV, 모니터</TD><TD>56.3%</TD></TR></TABLE>
                """;

        String sliced = slicer.slice(content);

        assertThat(sliced)
                .contains("| DX 부문 | TV, 모니터 | 56.3%")
                .contains("R&D 중심 기업")
                .doesNotContain("<TR")
                .doesNotContain("&amp;");
    }

    @Test
    @DisplayName("금융 서식은 사업의 개요와 영업의 현황 앞부분만 남기고 절단한다")
    void shouldSliceFinancialSectionsWithTruncation() {
        String longTable = "<TR><TD>예수금</TD><TD>593,281</TD></TR>".repeat(500);
        String content = """
                <TITLE ATOC="Y">1. 사업의 개요</TITLE>
                <P>인터넷전문은행입니다.</P>
                <TITLE ATOC="Y">2. 영업의 현황</TITLE>
                <P>모바일 앱 기반 여수신 상품을 제공합니다.</P>
                <TABLE>%s<TR><TD>조달합계-마지막행</TD></TR></TABLE>
                <TITLE ATOC="Y">3. 파생상품거래 현황</TITLE>
                <P>통화스왑 내역</P>
                <TITLE ATOC="Y">4. 영업설비</TITLE>
                <P>본점 현황</P>
                <TITLE ATOC="Y">5. 재무건전성 등 기타 참고사항</TITLE>
                <P>자본비율</P>
                """.formatted(longTable);

        String sliced = slicer.slice(content);

        assertThat(sliced)
                .contains("인터넷전문은행입니다.")
                .contains("모바일 앱 기반 여수신 상품을 제공합니다.")
                .contains("…(이하 생략)")
                .doesNotContain("조달합계-마지막행")
                .doesNotContain("통화스왑 내역")
                .doesNotContain("본점 현황")
                .doesNotContain("자본비율");
    }

    @Test
    @DisplayName("혼합 서식은 파트별로 나눠 각각의 선별 규칙을 적용한다")
    void shouldSliceMixedFormatByPart() {
        String content = """
                <TITLE ATOC="Y">1. (제조서비스업)사업의 개요</TITLE>
                <P>플랫폼 사업 개요</P>
                <TITLE ATOC="Y">2. (제조서비스업)주요 제품 및 서비스</TITLE>
                <P>광고, 커머스</P>
                <TITLE ATOC="Y">3. (제조서비스업)원재료 및 생산설비</TITLE>
                <P>서버 설비</P>
                <TITLE ATOC="Y">1. (금융업)사업의 개요</TITLE>
                <P>간편결제 사업 개요</P>
                <TITLE ATOC="Y">2. (금융업)영업의 현황</TITLE>
                <P>결제 취급액 현황</P>
                <TITLE ATOC="Y">3. (금융업)파생상품거래 현황</TITLE>
                <P>해당사항 없음</P>
                """;

        String sliced = slicer.slice(content);

        assertThat(sliced)
                .contains("[제조서비스업]")
                .contains("플랫폼 사업 개요")
                .contains("광고, 커머스")
                .contains("[금융업]")
                .contains("간편결제 사업 개요")
                .contains("결제 취급액 현황")
                .doesNotContain("서버 설비")
                .doesNotContain("해당사항 없음");
    }

    @Test
    @DisplayName("번호 붙은 하위 항목이 없으면 예외를 던진다")
    void shouldThrowWhenNoNumberedHeadings() {
        assertThatThrownBy(() -> slicer.slice("<TITLE>II. 사업의 내용</TITLE> 본문만 있음"))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("하위 항목");
    }

    @Test
    @DisplayName("사업의 개요가 선별되지 않으면 다른 항목이 있어도 예외를 던진다")
    void shouldThrowWhenOverviewMissing() {
        String content = """
                <TITLE ATOC="Y">3. 원재료 및 생산설비</TITLE>
                <P>설비 현황</P>
                <TITLE ATOC="Y">4. 매출 및 수주상황</TITLE>
                <P>매출 실적</P>
                """;

        assertThatThrownBy(() -> slicer.slice(content))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("사업의 개요");
    }
}
