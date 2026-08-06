package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportListing;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import dev.gukin.einvestlab.research.domain.AnalystReportSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("애널리스트 리포트 수집 유스케이스 단위 테스트")
class AnalystReportCollectUnitTest {

    private static final Instant BASE_TIME = Instant.parse("2026-08-05T03:00:00Z");

    private final StubSource source = new StubSource();
    private final StubRepository repository = new StubRepository();
    private final AnalystReportCollectUseCase useCase =
            new AnalystReportCollectUseCase(source, repository);

    @Test
    @DisplayName("신규 리포트는 저장하고 이미 수집한 리포트는 건너뛴다")
    void shouldCollectNewAndSkipExisting() {
        source.listings = List.of(listing(1L), listing(2L));
        repository.existing = 1L;

        AnalystReportCollectResult result =
                useCase.collect(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), BASE_TIME);

        assertThat(result).isEqualTo(new AnalystReportCollectResult(1, 1));
        assertThat(repository.saved).hasSize(1);
        AnalystReport saved = repository.saved.getFirst();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getReportIdx()).isEqualTo(2L);
        assertThat(saved.getStockCode()).isEqualTo("016360");
        assertThat(saved.getTargetPrice()).isEqualTo(115_000L);
        assertThat(saved.getCollectedAt()).isEqualTo(BASE_TIME);
    }

    @Test
    @DisplayName("기간을 안 주면 기준 시각의 한국 날짜로 최근 7일을 조회한다")
    void shouldDefaultToLastSevenDaysInKoreanDate() {
        source.listings = List.of();

        useCase.collect(null, null, BASE_TIME);

        assertThat(source.requestedEnd).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(source.requestedStart).isEqualTo(LocalDate.of(2026, 7, 29));
    }

    @Test
    @DisplayName("시작일이 종료일보다 뒤면 거부한다")
    void shouldRejectInvertedWindow() {
        assertThatThrownBy(() ->
                useCase.collect(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 1), BASE_TIME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("역전");
    }

    private AnalystReportListing listing(long reportIdx) {
        return new AnalystReportListing(reportIdx, "016360", "삼성증권",
                "삼성증권(016360) 최대실적 지속 경신", "LS증권", "전배승",
                LocalDate.of(2026, 8, 5), 115_000L, "Hold");
    }

    private static class StubSource implements AnalystReportSource {

        private List<AnalystReportListing> listings = List.of();
        private LocalDate requestedStart;
        private LocalDate requestedEnd;

        @Override
        public List<AnalystReportListing> fetchListings(LocalDate start, LocalDate end) {
            this.requestedStart = start;
            this.requestedEnd = end;
            return listings;
        }
    }

    private static class StubRepository implements AnalystReportRepository {

        private final List<AnalystReport> saved = new ArrayList<>();
        private Long existing;

        @Override
        public AnalystReport save(AnalystReport analystReport) {
            saved.add(analystReport);
            return analystReport;
        }

        @Override
        public boolean existsByReportIdx(long reportIdx) {
            return existing != null && existing == reportIdx;
        }

        @Override
        public List<AnalystReport> findAllWithoutPdf() {
            return List.of();
        }

        @Override
        public List<AnalystReport> findAllPendingEpsExtraction() {
            return List.of();
        }
    }
}
