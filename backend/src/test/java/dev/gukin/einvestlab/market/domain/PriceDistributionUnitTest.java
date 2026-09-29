package dev.gukin.einvestlab.market.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("주가 분포 단위 테스트")
class PriceDistributionUnitTest {

    private static final int TARGET_BINS = 24;

    @Test
    @DisplayName("구간 경계를 1·2·5 배수로 떨어뜨리고 최저가가 속한 배수에서 시작한다")
    void shouldBucketOnNiceBoundaries() {
        List<DailyStockPrice> prices = ladder(60_000, 100, 121);

        PriceDistribution distribution = PriceDistribution.of(prices, TARGET_BINS).orElseThrow();

        assertThat(distribution.bins()).hasSize(25);
        assertThat(distribution.bins().getFirst()).isEqualTo(new PriceDistribution.Bin(60_000, 60_500, 5));
        assertThat(distribution.bins().getLast().to()).isEqualTo(72_500);
        assertThat(distribution.minPrice()).isEqualTo(60_000);
        assertThat(distribution.maxPrice()).isEqualTo(72_000);
    }

    @Test
    @DisplayName("모든 거래일이 정확히 한 구간에만 들어간다")
    void shouldCountEveryTradingDayOnce() {
        List<DailyStockPrice> prices = ladder(50_000, 9_900, 100);

        PriceDistribution distribution = PriceDistribution.of(prices, TARGET_BINS).orElseThrow();

        assertThat(distribution.bins()).extracting(PriceDistribution.Bin::count)
                .satisfies(counts -> assertThat(counts.stream().mapToInt(Integer::intValue).sum())
                        .isEqualTo(distribution.tradingDays()));
        assertThat(distribution.tradingDays()).isEqualTo(100);
    }

    @Test
    @DisplayName("평균은 소수점 둘째 자리까지 반올림한 파생값이다")
    void shouldRoundMeanToTwoDecimals() {
        PriceDistribution distribution =
                PriceDistribution.of(prices(70_000, 70_001), TARGET_BINS).orElseThrow();

        assertThat(distribution.mean()).isEqualByComparingTo(new BigDecimal("70000.50"));
    }

    @Test
    @DisplayName("구간 내 주가가 모두 같으면 구간 하나로 접힌다")
    void shouldCollapseToSingleBinWhenPricesAreEqual() {
        PriceDistribution distribution =
                PriceDistribution.of(prices(70_000, 70_000, 70_000), TARGET_BINS).orElseThrow();

        assertThat(distribution.bins()).hasSize(1);
        assertThat(distribution.bins().getFirst().count()).isEqualTo(3);
        assertThat(distribution.mean()).isEqualByComparingTo(new BigDecimal("70000"));
    }

    @Test
    @DisplayName("거래정지로 종가가 0 인 날은 분포와 평균에서 제외한다")
    void shouldIgnoreHaltedDays() {
        PriceDistribution distribution =
                PriceDistribution.of(prices(70_000, 0, 71_000, 0, 72_000), TARGET_BINS).orElseThrow();

        assertThat(distribution.tradingDays()).isEqualTo(3);
        assertThat(distribution.minPrice()).isEqualTo(70_000);
        assertThat(distribution.mean()).isEqualByComparingTo(new BigDecimal("71000"));
    }

    @Test
    @DisplayName("거래일이 없으면 분포가 비어 있다")
    void shouldBeEmptyWithoutTradingDays() {
        assertThat(PriceDistribution.of(List.of(), TARGET_BINS)).isEmpty();
        assertThat(PriceDistribution.of(prices(0, 0), TARGET_BINS)).isEmpty();
    }

    @Test
    @DisplayName("구간 개수가 1 미만이면 거부한다")
    void shouldRejectNonPositiveTargetBins() {
        assertThatThrownBy(() -> PriceDistribution.of(prices(70_000), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<DailyStockPrice> ladder(int start, int stepAmount, int days) {
        return IntStream.range(0, days)
                .mapToObj(index -> price(start + index * stepAmount))
                .toList();
    }

    private static List<DailyStockPrice> prices(int... closePrices) {
        return IntStream.of(closePrices).mapToObj(PriceDistributionUnitTest::price).toList();
    }

    private static DailyStockPrice price(int closePrice) {
        return DailyStockPrice.builder()
                .stockCode("005930")
                .tradeDate(LocalDate.of(2024, 1, 1))
                .marketCategory("KOSPI")
                .openPrice(closePrice)
                .highPrice(closePrice)
                .lowPrice(closePrice)
                .closePrice(closePrice)
                .volume(0)
                .collectedAt(Instant.EPOCH)
                .build();
    }
}
