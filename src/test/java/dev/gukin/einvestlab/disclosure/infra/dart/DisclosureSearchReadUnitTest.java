package dev.gukin.einvestlab.disclosure.infra.dart;

import dev.gukin.einvestlab.disclosure.domain.BusinessReportFiling;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DART 공시검색 응답 해석 단위 테스트")
class DisclosureSearchReadUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("여러 건 중 접수일이 가장 최근인 사업보고서 1건을 선택한다")
    void shouldPickLatestFiling() {
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

        Optional<BusinessReportFiling> filing = read(body).toFiling("00126380");

        assertThat(filing).contains(new BusinessReportFiling(
                "00126380",
                "20260310002820",
                LocalDate.of(2026, 3, 10)
        ));
    }

    @Test
    @DisplayName("조회 결과 없음(status 013)은 빈 결과로 매핑한다")
    void shouldMapNoResultToEmpty() {
        String body = """
                {"status": "013", "message": "조회된 데이타가 없습니다."}
                """;

        Optional<BusinessReportFiling> filing = read(body).toFiling("00126380");

        assertThat(filing).isEmpty();
    }

    @Test
    @DisplayName("정상·결과없음 외의 status 는 예외를 던진다")
    void shouldThrowOnErrorStatus() {
        String body = """
                {"status": "020", "message": "요청 제한을 초과하였습니다."}
                """;

        DisclosureSearchResponse response = read(body);

        assertThatThrownBy(() -> response.toFiling("00126380"))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("020");
    }

    @Test
    @DisplayName("status 000 이지만 목록이 비어 있으면 빈 결과로 매핑한다")
    void shouldMapEmptyListToEmpty() {
        String body = """
                {"status": "000", "message": "정상", "list": []}
                """;

        Optional<BusinessReportFiling> filing = read(body).toFiling("00126380");

        assertThat(filing).isEmpty();
    }

    private DisclosureSearchResponse read(String body) {
        try {
            return objectMapper.readValue(body, DisclosureSearchResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("테스트 JSON 파싱 실패", e);
        }
    }
}
