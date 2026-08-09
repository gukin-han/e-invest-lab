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
    @DisplayName("값 없이 저장된 기존 행을 재수집하면 상장주식수·시가총액이 채워진다 — 백필 경로")
    void shouldBackfillShareCountOnExistingRows() {
        repository.upsertPrices(List.of(price("005930", LocalDate.of(2026, 8, 6), 70_000)));
        assertThat(repository.findLatestByStockCode("005930").orElseThrow().getListedShareCount())
                .isNull();

        DailyStockPrice recollected = DailyStockPrice.builder()
                .id(Ids.generate())
                .stockCode("005930")
                .tradeDate(LocalDate.of(2026, 8, 6))
                .marketCategory("KOSPI")
                .openPrice(69_500)
                .highPrice(70_500)
                .lowPrice(69_000)
                .closePrice(70_000)
                .volume(1_000L)
                .listedShareCount(5_846_278_608L)
                .marketCap(1_347_567_219_144_000L)
                .collectedAt(Instant.parse("2026-08-09T09:00:00Z"))
                .build();
        repository.upsertPrices(List.of(recollected));

        assertThat(jpa.count()).isEqualTo(1);
        DailyStockPrice row = repository.findLatestByStockCode("005930").orElseThrow();
        assertThat(row.getListedShareCount()).isEqualTo(5_846_278_608L);
        assertThat(row.getMarketCap()).isEqualTo(1_347_567_219_144_000L);
    }

    @Test
    @DisplayName("시계열 조회는 기간 안의 그 종목만 거래일 오름차순으로 준다")
    void shouldFindSeriesWithinWindowOrdered() {
        repository.upsertPrices(List.of(
                price("005930", LocalDate.of(2026, 8, 4), 68_000),
                price("005930", LocalDate.of(2026, 8, 6), 70_000),
                price("005930", LocalDate.of(2026, 7, 1), 65_000),
                price("000660", LocalDate.of(2026, 8, 5), 250_000)));

        assertThat(repository.findSeries("005930", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .extracting(DailyStockPrice::getTradeDate, DailyStockPrice::getClosePrice)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 8, 4), 68_000),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 8, 6), 70_000));
    }

    @Test
    @DisplayName("주식수 변화 랭킹은 순변화율·감소 누적을 계산하고 감소순 정렬한다")
    void shouldRankShareCountTrends() {
        repository.upsertPrices(List.of(
                priceWithShares("111111", LocalDate.of(2026, 8, 1), 1_000_000L),
                priceWithShares("111111", LocalDate.of(2026, 8, 4), 950_000L),
                priceWithShares("111111", LocalDate.of(2026, 8, 6), 900_000L),
                priceWithShares("222222", LocalDate.of(2026, 8, 1), 1_000_000L),
                priceWithShares("222222", LocalDate.of(2026, 8, 6), 1_200_000L)));

        var trends = repository.findShareCountTrends(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 1), true, null, 10);

        assertThat(trends).hasSize(2);
        var top = trends.getFirst();
        assertThat(top.stockCode()).isEqualTo("111111");
        assertThat(top.startCount()).isEqualTo(1_000_000L);
        assertThat(top.endCount()).isEqualTo(900_000L);
        assertThat(top.netChangePct()).isEqualByComparingTo("-10.00");
        assertThat(top.decreaseEvents()).isEqualTo(2);
        assertThat(top.decreasedShares()).isEqualTo(100_000L);
        assertThat(top.lastDecreaseDate()).isEqualTo(LocalDate.of(2026, 8, 6));
        assertThat(trends.getLast().netChangePct()).isEqualByComparingTo("20.00");
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

    private DailyStockPrice priceWithShares(String stockCode, LocalDate tradeDate, long shareCount) {
        return DailyStockPrice.builder()
                .id(Ids.generate())
                .stockCode(stockCode)
                .tradeDate(tradeDate)
                .marketCategory("KOSPI")
                .openPrice(10_000)
                .highPrice(10_500)
                .lowPrice(9_500)
                .closePrice(10_000)
                .volume(1_000L)
                .listedShareCount(shareCount)
                .marketCap(shareCount * 10_000)
                .collectedAt(Instant.parse("2026-08-07T03:00:00Z"))
                .build();
    }
}
