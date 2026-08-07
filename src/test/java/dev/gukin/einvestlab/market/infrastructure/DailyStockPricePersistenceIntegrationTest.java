package dev.gukin.einvestlab.market.infrastructure;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.market.infrastructure.persistence.DailyStockPriceJpaRepository;
import dev.gukin.einvestlab.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("일별 주가 영속화 통합 테스트")
class DailyStockPricePersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DailyStockPriceRepository repository;

    @Autowired
    private DailyStockPriceJpaRepository jpa;

    @AfterEach
    void tearDown() {
        jpa.deleteAllInBatch();
    }

    @Test
    @DisplayName("같은 종목·거래일을 다시 저장하면 행이 늘지 않고 값이 갱신된다")
    void shouldUpsertIdempotently() {
        repository.upsertPrices(List.of(price("005930", LocalDate.of(2026, 8, 6), 70_000)));
        repository.upsertPrices(List.of(price("005930", LocalDate.of(2026, 8, 6), 70_500)));

        assertThat(jpa.count()).isEqualTo(1);
        assertThat(repository.findLatestByStockCode("005930").orElseThrow().getClosePrice())
                .isEqualTo(70_500);
    }

    @Test
    @DisplayName("최근 시세는 거래일 기준 가장 늦은 행이다")
    void shouldFindLatestByTradeDate() {
        repository.upsertPrices(List.of(
                price("005930", LocalDate.of(2026, 8, 5), 69_000),
                price("005930", LocalDate.of(2026, 8, 6), 70_000),
                price("000660", LocalDate.of(2026, 8, 6), 250_000)));

        DailyStockPrice latest = repository.findLatestByStockCode("005930").orElseThrow();
        assertThat(latest.getTradeDate()).isEqualTo(LocalDate.of(2026, 8, 6));
        assertThat(latest.getClosePrice()).isEqualTo(70_000);
        assertThat(repository.findLatestByStockCode("999999")).isEmpty();
    }

    private DailyStockPrice price(String stockCode, LocalDate tradeDate, int closePrice) {
        return DailyStockPrice.builder()
                .id(Ids.generate())
                .stockCode(stockCode)
                .tradeDate(tradeDate)
                .marketCategory("KOSPI")
                .openPrice(closePrice - 500)
                .highPrice(closePrice + 500)
                .lowPrice(closePrice - 1_000)
                .closePrice(closePrice)
                .volume(1_000_000L)
                .collectedAt(Instant.parse("2026-08-07T03:00:00Z"))
                .build();
    }
}
