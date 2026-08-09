package dev.gukin.einvestlab.market.application;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.market.domain.ShareCountTrend;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("주가 시계열 조회 서비스 단위 테스트")
class StockPriceQueryUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-08-08T03:00:00Z");

    private final RecordingRepository repository = new RecordingRepository();
    private final StockPriceQuery query = new StockPriceQuery(repository);

    @Test
    @DisplayName("기간을 안 주면 한국 날짜 기준 최근 90일이다")
    void shouldDefaultToNinetyDays() {
        query.series("005930", null, null, BASE_TIME);

        assertThat(repository.requestedFrom).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(repository.requestedTo).isEqualTo(LocalDate.of(2026, 8, 8));
    }

    @Test
    @DisplayName("시작일이 종료일보다 뒤면 거부한다")
    void shouldRejectInvertedWindow() {
        assertThatThrownBy(() -> query.series("005930",
                LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 1), BASE_TIME))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static class RecordingRepository implements DailyStockPriceRepository {

        private LocalDate requestedFrom;
        private LocalDate requestedTo;

        @Override
        public int upsertPrices(List<DailyStockPrice> prices) {
            return prices.size();
        }

        @Override
        public Optional<DailyStockPrice> findLatestByStockCode(String stockCode) {
            return Optional.empty();
        }

        @Override
        public List<DailyStockPrice> findSeries(String stockCode, LocalDate from, LocalDate to) {
            this.requestedFrom = from;
            this.requestedTo = to;
            return List.of();
        }

        @Override
        public int rebuildShareCountChanges(java.time.Instant computedAt) {
            return 0;
        }

        @Override
        public List<ShareCountTrend> findShareCountTrends(LocalDate since, LocalDate listedCutoff,
                                                          boolean decreasing, java.math.BigDecimal maxSingleDropPct,
                                                          java.math.BigDecimal maxSingleRisePct, int limit) {
            return List.of();
        }
    }
}
