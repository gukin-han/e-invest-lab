package dev.gukin.einvestlab.market.application;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.DailyStockPriceEntry;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.market.domain.DailyStockPriceSource;
import dev.gukin.einvestlab.support.RecordingTransactionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("일별 주가 수집 유스케이스 단위 테스트")
class DailyStockPriceCollectUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-08-07T03:00:00Z");

    private final StubSource source = new StubSource();
    private final StubRepository repository = new StubRepository();
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();
    private final DailyStockPriceCollectUseCase useCase =
            new DailyStockPriceCollectUseCase(source, repository, transactionManager);

    @Test
    @DisplayName("거래일만 날짜 단위 트랜잭션으로 저장하고 휴일은 건너뛴다")
    void shouldUpsertTradingDaysAndSkipHolidays() {
        source.entriesByDate.put(LocalDate.of(2026, 8, 5), List.of(entry("005930"), entry("000660")));
        source.entriesByDate.put(LocalDate.of(2026, 8, 6), List.of(entry("005930")));

        DailyStockPriceCollectResult result =
                useCase.collect(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 7), BASE_TIME);

        assertThat(result).isEqualTo(new DailyStockPriceCollectResult(3, 2));
        assertThat(source.requestedDates).hasSize(7);
        assertThat(transactionManager.startedCount()).isEqualTo(2);
        assertThat(repository.upserted)
                .extracting(DailyStockPrice::getStockCode, DailyStockPrice::getCollectedAt)
                .contains(org.assertj.core.groups.Tuple.tuple("005930", BASE_TIME));
        assertThat(repository.upserted).extracting(DailyStockPrice::getId).doesNotContainNull();
    }

    @Test
    @DisplayName("기간을 안 주면 기준 시각의 한국 날짜로 최근 7일을 순회한다")
    void shouldDefaultToLastSevenDaysInKoreanDate() {
        useCase.collect(null, null, BASE_TIME);

        assertThat(source.requestedDates.getFirst()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(source.requestedDates.getLast()).isEqualTo(LocalDate.of(2026, 8, 7));
    }

    @Test
    @DisplayName("시작일이 종료일보다 뒤면 거부한다")
    void shouldRejectInvertedWindow() {
        assertThatThrownBy(() ->
                useCase.collect(LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 1), BASE_TIME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("역전");
    }

    private DailyStockPriceEntry entry(String stockCode) {
        return new DailyStockPriceEntry(stockCode, LocalDate.of(2026, 8, 5), "KOSPI",
                70_000, 71_000, 69_500, 70_500, 1_234_567L, 5_969_782_550L, 420_000_000_000_000L);
    }

    private static class StubSource implements DailyStockPriceSource {

        private final Map<LocalDate, List<DailyStockPriceEntry>> entriesByDate = new HashMap<>();
        private final List<LocalDate> requestedDates = new ArrayList<>();

        @Override
        public List<DailyStockPriceEntry> fetchAll(LocalDate tradeDate) {
            requestedDates.add(tradeDate);
            return entriesByDate.getOrDefault(tradeDate, List.of());
        }
    }

    private static class StubRepository implements DailyStockPriceRepository {

        private final List<DailyStockPrice> upserted = new ArrayList<>();

        @Override
        public int upsertPrices(List<DailyStockPrice> prices) {
            upserted.addAll(prices);
            return prices.size();
        }

        @Override
        public Optional<DailyStockPrice> findLatestByStockCode(String stockCode) {
            return Optional.empty();
        }

        @Override
        public List<DailyStockPrice> findSeries(String stockCode, LocalDate from, LocalDate to) {
            return List.of();
        }
    }
}
