package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.research.domain.EpsConsensus;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsRevision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EPS 통계 조회 서비스 단위 테스트")
class EpsStatisticsQueryUnitTest {

    private final RecordingRepository repository = new RecordingRepository();
    private final EpsStatisticsQuery query = new EpsStatisticsQuery(repository);

    @Test
    @DisplayName("유효기간은 기준 시각의 한국 날짜에서 6개월 전이다")
    void shouldComputeSinceAsSixMonthsBeforeKoreanDate() {
        query.consensus("016360", Instant.parse("2026-08-07T03:00:00Z"));

        assertThat(repository.requestedSince).isEqualTo(LocalDate.of(2026, 2, 7));
        assertThat(repository.requestedStockCode).isEqualTo("016360");
    }

    @Test
    @DisplayName("자정 직전 UTC 시각도 한국 날짜 기준으로 해석한다")
    void shouldInterpretBaseTimeInKoreanZone() {
        query.consensus("016360", Instant.parse("2026-08-06T16:00:00Z"));

        assertThat(repository.requestedSince).isEqualTo(LocalDate.of(2026, 2, 7));
    }

    private static class RecordingRepository implements EpsEstimateRepository {

        private String requestedStockCode;
        private LocalDate requestedSince;

        @Override
        public void saveAll(List<EpsEstimate> estimates) {
        }

        @Override
        public List<EpsConsensus> findConsensus(String stockCode, LocalDate since) {
            this.requestedStockCode = stockCode;
            this.requestedSince = since;
            return List.of();
        }

        @Override
        public List<EpsRevision> findRevisions(String stockCode, int fiscalYear) {
            return List.of();
        }
    }
}
