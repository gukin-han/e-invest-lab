package dev.gukin.einvestlab.disclosure.infra.dart;

import dev.gukin.einvestlab.disclosure.domain.BusinessReportFiling;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DART 공시검색 응답 해석 단위 테스트")
class DisclosureSearchReadUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("접수일 최신순으로 정렬한 사업보고서 목록을 돌려준다")
    void shouldReturnFilingsNewestFirst() {
        String body = """
                {
                    "status": "000",
                    "message": "정상",
                    "list": [
                        {"rcept_no": "20250311001234", "rcept_dt": "20250311", "report_nm": "사업보고서 (2024.12)"},
                        {"rcept_no": "20260310002820", "rcept_dt": "20260310", "report_nm": "사업보고서 (2025.12)"}
                    ]
                }
                """;

        List<BusinessReportFiling> filings = read(body).toFilings("00126380");

        assertThat(filings).containsExactly(
                new BusinessReportFiling("00126380", "20260310002820", LocalDate.of(2026, 3, 10)),
                new BusinessReportFiling("00126380", "20250311001234", LocalDate.of(2025, 3, 11))
        );
    }

    @Test
    @DisplayName("같은 접수일이면 접수번호가 큰 쪽을 앞세운다")
    void shouldBreakSameDateTieByFilingNumber() {
        String body = """
                {
                    "status": "000",
                    "message": "정상",
                    "list": [
                        {"rcept_no": "20260324000822", "rcept_dt": "20260324"},
                        {"rcept_no": "20260324000835", "rcept_dt": "20260324"}
                    ]
                }
                """;

        List<BusinessReportFiling> filings = read(body).toFilings("00688996");

        assertThat(filings)
                .extracting(BusinessReportFiling::filingNumber)
                .containsExactly("20260324000835", "20260324000822");
    }

    @Test
    @DisplayName("조회 결과 없음(status 013)은 빈 목록으로 매핑한다")
    void shouldMapNoResultToEmpty() {
        String body = """
                {"status": "013", "message": "조회된 데이타가 없습니다."}
                """;

        assertThat(read(body).toFilings("00126380")).isEmpty();
    }

    @Test
    @DisplayName("정상·결과없음 외의 status 는 예외를 던진다")
    void shouldThrowOnErrorStatus() {
        String body = """
                {"status": "020", "message": "요청 제한을 초과하였습니다."}
                """;

        DisclosureSearchResponse response = read(body);

        assertThatThrownBy(() -> response.toFilings("00126380"))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("020");
    }

    @Test
    @DisplayName("status 000 이지만 목록이 비어 있으면 빈 목록으로 매핑한다")
    void shouldMapEmptyListToEmpty() {
        String body = """
                {"status": "000", "message": "정상", "list": []}
                """;

        assertThat(read(body).toFilings("00126380")).isEmpty();
    }

    private DisclosureSearchResponse read(String body) {
        return objectMapper.readValue(body, DisclosureSearchResponse.class);
    }
}
