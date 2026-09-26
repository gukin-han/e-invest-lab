package dev.gukin.einvestlab.research.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("종목 커버리지 조회 서비스 단위 테스트")
class StockCoverageQueryUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-08-08T03:00:00Z");

    private final StubAnalystReportRepository repository = new StubAnalystReportRepository();
    private final StockCoverageQuery query = new StockCoverageQuery(repository);

    @Test
    @DisplayName("기간을 안 주면 한국 날짜 기준 최근 7일이다")
    void shouldDefaultToSevenDays() {
        query.recentlyCovered(null, BASE_TIME);

        assertThat(repository.requestedCoveredSince).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    @DisplayName("지정한 기간만큼 거슬러 조회한다")
    void shouldUseRequestedDays() {
        query.recentlyCovered(30, BASE_TIME);

        assertThat(repository.requestedCoveredSince).isEqualTo(LocalDate.of(2026, 7, 9));
    }

    @Test
    @DisplayName("허용 범위 밖 기간은 거부한다")
    void shouldRejectOutOfRangeDays() {
        assertThatThrownBy(() -> query.recentlyCovered(0, BASE_TIME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> query.recentlyCovered(91, BASE_TIME))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
