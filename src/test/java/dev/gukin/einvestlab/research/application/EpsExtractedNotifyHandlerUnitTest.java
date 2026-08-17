package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.market.domain.ShareCountTrend;
import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.EpsConsensus;
import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.domain.EpsExtractedEvent;
import dev.gukin.einvestlab.research.domain.EpsFigure;
import dev.gukin.einvestlab.research.domain.EpsNotification;
import dev.gukin.einvestlab.research.domain.EpsNotifier;
import dev.gukin.einvestlab.research.domain.EpsRevision;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EPS 추출 이벤트 알림 핸들러 단위 테스트")
class EpsExtractedNotifyHandlerUnitTest {

    private static final LocalDate PUBLISHED = LocalDate.of(2026, 8, 17);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StubAnalystReportRepository reportRepository = new StubAnalystReportRepository();
    private final RecordingEstimateRepository estimateRepository = new RecordingEstimateRepository();
    private final List<EpsNotification> notified = new ArrayList<>();
    private final EpsExtractedNotifyHandler handler = new EpsExtractedNotifyHandler(
            new EpsNotificationComposer(reportRepository, estimateRepository, new StubPriceRepository()),
            (EpsNotifier) notified::add, objectMapper);

    @Test
    @DisplayName("EPS_EXTRACTED 타입만 지원한다")
    void shouldSupportOnlyEpsExtracted() {
        assertThat(handler.supports("EPS_EXTRACTED")).isTrue();
        assertThat(handler.supports("OTHER")).isFalse();
    }

    @Test
    @DisplayName("페이로드를 되살린 뒤 리포트 메타·직전 리포트·증권사 제외 컨센서스·시세로 보강해 알림 포트에 넘긴다")
    void shouldComposeAndNotify() {
        reportRepository.byReportIdx = report(2L, PUBLISHED, 82_000L, "Buy");
        reportRepository.previousByBroker = report(1L, PUBLISHED.minusMonths(1), 72_000L, "Buy");
        estimateRepository.byReportIdx = List.of(estimate(1L, 2026, "8850"));
        estimateRepository.consensuses = List.of(new EpsConsensus(2026, new BigDecimal("9100"), 3, null, null));
        List<EpsFigure> figures = List.of(new EpsFigure(2026, true, new BigDecimal("9599")));

        handler.handle(OutboxEvent.pending(UUID.randomUUID(), EpsExtractedEvent.TYPE, "2",
                objectMapper.writeValueAsString(new EpsExtractedEvent(2L, "192080", "더블유게임즈", figures)),
                Instant.EPOCH));

        assertThat(notified).hasSize(1);
        EpsNotification n = notified.getFirst();
        assertThat(n.reportIdx()).isEqualTo(2L);
        assertThat(n.broker()).isEqualTo("키움증권");
        assertThat(n.targetPrice()).isEqualTo(82_000L);
        assertThat(n.figures()).isEqualTo(figures);
        assertThat(n.previous().reportIdx()).isEqualTo(1L);
        assertThat(n.previous().figures()).containsExactly(new EpsFigure(2026, true, new BigDecimal("8850")));
        assertThat(n.consensus()).hasSize(1);
        assertThat(n.latestPrice()).isEqualTo(new EpsNotification.LatestPrice(58_000, PUBLISHED));
        assertThat(estimateRepository.excludedBroker).isEqualTo("키움증권");
        assertThat(estimateRepository.since).isEqualTo(PUBLISHED.minusMonths(6));
    }

    private static AnalystReport report(long reportIdx, LocalDate publishedDate, Long targetPrice, String opinion) {
        return AnalystReport.builder()
                .id(Ids.generate()).reportIdx(reportIdx).stockCode("192080").companyName("더블유게임즈")
                .title("더블유게임즈(192080)").broker("키움증권").publishedDate(publishedDate)
                .targetPrice(targetPrice).opinion(opinion).collectedAt(Instant.EPOCH)
                .build();
    }

    private static EpsEstimate estimate(long reportIdx, int fiscalYear, String eps) {
        return EpsEstimate.builder()
                .id(Ids.generate()).reportIdx(reportIdx).fiscalYear(fiscalYear).estimated(true)
                .eps(new BigDecimal(eps)).extractedAt(Instant.EPOCH)
                .build();
    }

    private static class RecordingEstimateRepository implements EpsEstimateRepository {
        List<EpsEstimate> byReportIdx = List.of();
        List<EpsConsensus> consensuses = List.of();
        String excludedBroker;
        LocalDate since;

        @Override
        public void saveAll(List<EpsEstimate> estimates) {
        }

        @Override
        public void deleteAllByReportIdx(long reportIdx) {
        }

        @Override
        public List<EpsEstimate> findAllByReportIdx(long reportIdx) {
            return byReportIdx;
        }

        @Override
        public List<EpsConsensus> findConsensus(String stockCode, LocalDate since, String excludedBroker) {
            this.since = since;
            this.excludedBroker = excludedBroker;
            return consensuses;
        }

        @Override
        public List<EpsRevision> findRevisions(String stockCode, int fiscalYear) {
            return List.of();
        }
    }

    private static class StubPriceRepository implements DailyStockPriceRepository {
        @Override
        public Optional<DailyStockPrice> findLatestByStockCode(String stockCode) {
            return Optional.of(DailyStockPrice.builder()
                    .id(Ids.generate()).stockCode(stockCode).tradeDate(PUBLISHED).marketCategory("KOSDAQ")
                    .openPrice(58_000).highPrice(58_000).lowPrice(58_000).closePrice(58_000)
                    .volume(1L).collectedAt(Instant.EPOCH).build());
        }

        @Override
        public int upsertPrices(List<DailyStockPrice> prices) {
            return 0;
        }

        @Override
        public List<DailyStockPrice> findSeries(String stockCode, LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public int rebuildShareCountChanges(Instant computedAt) {
            return 0;
        }

        @Override
        public List<ShareCountTrend> findShareCountTrends(LocalDate since, LocalDate listedCutoff,
                                                          boolean decreasing, BigDecimal maxSingleDropPct,
                                                          BigDecimal maxSingleRisePct, int limit) {
            return List.of();
        }
    }
}
