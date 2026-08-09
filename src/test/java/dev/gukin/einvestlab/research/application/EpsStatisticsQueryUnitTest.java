package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.market.domain.ShareCountTrend;
import dev.gukin.einvestlab.research.domain.EpsConsensus;
import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.domain.EpsRevision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("EPS 통계 조회 서비스 단위 테스트")
class EpsStatisticsQueryUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-08-07T03:00:00Z");

    private final RecordingEstimateRepository estimateRepository = new RecordingEstimateRepository();
    private final StubPriceRepository priceRepository = new StubPriceRepository();
    private final EpsStatisticsQuery query = new EpsStatisticsQuery(estimateRepository, priceRepository);

    @Test
    @DisplayName("유효기간은 기준 시각의 한국 날짜에서 6개월 전이다")
    void shouldComputeSinceAsSixMonthsBeforeKoreanDate() {
        query.consensus("016360", BASE_TIME);

        assertThat(estimateRepository.requestedSince).isEqualTo(LocalDate.of(2026, 2, 7));
        assertThat(estimateRepository.requestedStockCode).isEqualTo("016360");
    }

    @Test
    @DisplayName("자정 직전 UTC 시각도 한국 날짜 기준으로 해석한다")
    void shouldInterpretBaseTimeInKoreanZone() {
        query.consensus("016360", Instant.parse("2026-08-06T16:00:00Z"));

        assertThat(estimateRepository.requestedSince).isEqualTo(LocalDate.of(2026, 2, 7));
    }

    @Test
    @DisplayName("최근 종가를 연도 컨센서스로 나눠 PER 을 계산하고, 지난 회계연도는 trailing·올해부터는 forward 로 판정한다")
    void shouldComputePerWithTrailingForwardClassification() {
        priceRepository.latest = price("016360", LocalDate.of(2026, 8, 6), 70_000);
        estimateRepository.consensuses = List.of(
                consensus(2025, "7000"),
                consensus(2026, "5000"),
                consensus(2027, "8000"));

        EpsConsensusResult result = query.consensus("016360", BASE_TIME);

        assertThat(result.latestClosePrice()).isEqualTo(70_000);
        assertThat(result.latestTradeDate()).isEqualTo(LocalDate.of(2026, 8, 6));
        assertThat(result.years())
                .extracting(EpsConsensusResult.YearlyConsensus::fiscalYear,
                        EpsConsensusResult.YearlyConsensus::per,
                        EpsConsensusResult.YearlyConsensus::perType)
                .containsExactly(
                        tuple(2025, new BigDecimal("10.00"), EpsConsensusResult.PerType.TRAILING),
                        tuple(2026, new BigDecimal("14.00"), EpsConsensusResult.PerType.FORWARD),
                        tuple(2027, new BigDecimal("8.75"), EpsConsensusResult.PerType.FORWARD)
                );
    }

    @Test
    @DisplayName("컨센서스가 0 이하(적자)면 PER 을 계산하지 않는다")
    void shouldSkipPerForNonPositiveConsensus() {
        priceRepository.latest = price("016360", LocalDate.of(2026, 8, 6), 70_000);
        estimateRepository.consensuses = List.of(consensus(2026, "-1500"));

        EpsConsensusResult result = query.consensus("016360", BASE_TIME);

        assertThat(result.years().getFirst().per()).isNull();
    }

    @Test
    @DisplayName("시세가 없으면 종가와 PER 없이 컨센서스만 준다")
    void shouldReturnConsensusWithoutPerWhenPriceMissing() {
        estimateRepository.consensuses = List.of(consensus(2026, "5000"));

        EpsConsensusResult result = query.consensus("016360", BASE_TIME);

        assertThat(result.latestClosePrice()).isNull();
        assertThat(result.latestTradeDate()).isNull();
        assertThat(result.years().getFirst().averageEps()).isEqualByComparingTo("5000");
        assertThat(result.years().getFirst().per()).isNull();
    }

    private EpsConsensus consensus(int fiscalYear, String averageEps) {
        return new EpsConsensus(fiscalYear, new BigDecimal(averageEps), 3,
                new BigDecimal(averageEps), new BigDecimal(averageEps));
    }

    private DailyStockPrice price(String stockCode, LocalDate tradeDate, int closePrice) {
        return DailyStockPrice.builder()
                .id(Ids.generate())
                .stockCode(stockCode)
                .tradeDate(tradeDate)
                .marketCategory("KOSPI")
                .openPrice(closePrice)
                .highPrice(closePrice)
                .lowPrice(closePrice)
                .closePrice(closePrice)
                .volume(1_000L)
                .collectedAt(BASE_TIME)
                .build();
    }

    private static class RecordingEstimateRepository implements EpsEstimateRepository {

        private String requestedStockCode;
        private LocalDate requestedSince;
        private List<EpsConsensus> consensuses = List.of();

        @Override
        public void saveAll(List<EpsEstimate> estimates) {
        }

        @Override
        public void deleteAllByReportIdx(long reportIdx) {
        }

        @Override
        public List<EpsConsensus> findConsensus(String stockCode, LocalDate since) {
            this.requestedStockCode = stockCode;
            this.requestedSince = since;
            return consensuses;
        }

        @Override
        public List<EpsRevision> findRevisions(String stockCode, int fiscalYear) {
            return List.of();
        }
    }

    private static class StubPriceRepository implements DailyStockPriceRepository {

        private DailyStockPrice latest;

        @Override
        public int upsertPrices(List<DailyStockPrice> prices) {
            return prices.size();
        }

        @Override
        public Optional<DailyStockPrice> findLatestByStockCode(String stockCode) {
            return Optional.ofNullable(latest);
        }

        @Override
        public List<DailyStockPrice> findSeries(String stockCode, LocalDate from, LocalDate to) {
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
