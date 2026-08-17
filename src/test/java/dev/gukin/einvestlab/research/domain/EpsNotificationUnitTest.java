package dev.gukin.einvestlab.research.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EPS 알림 파생값 단위 테스트")
class EpsNotificationUnitTest {

    private static final LocalDate PUBLISHED = LocalDate.of(2026, 8, 17);

    private static final List<EpsFigure> FIGURES = List.of(
            new EpsFigure(2027, true, new BigDecimal("11858")),
            new EpsFigure(2024, false, new BigDecimal("8708")),
            new EpsFigure(2026, true, new BigDecimal("9599")),
            new EpsFigure(2025, false, new BigDecimal("6106")),
            new EpsFigure(2028, true, new BigDecimal("12792")));

    @Test
    @DisplayName("기준 연도는 첫 추정 연도, 리비전·컨센서스 갭·목표주가 변화·PER 을 그 연도로 계산한다")
    void shouldDeriveFromKeyYear() {
        EpsNotification n = notification(
                new EpsNotification.PreviousReport(1L, PUBLISHED.minusMonths(1), 72_000L, "Buy",
                        List.of(new EpsFigure(2026, true, new BigDecimal("8850")))),
                List.of(new EpsConsensus(2026, new BigDecimal("9100"), 4, null, null)),
                58_000);

        assertThat(n.keyYear()).isEqualTo(2026);
        assertThat(n.figures()).extracting(EpsFigure::fiscalYear).containsExactly(2024, 2025, 2026, 2027, 2028);
        assertThat(n.revisionRate()).contains(new BigDecimal("8.5"));
        assertThat(n.signal()).isEqualTo(RevisionSignal.UP);
        assertThat(n.consensusGapRate()).contains(new BigDecimal("5.5"));
        assertThat(n.targetPriceChangeRate()).contains(new BigDecimal("13.9"));
        assertThat(n.forwardPer()).contains(new BigDecimal("6.0"));
        assertThat(n.yearOverYearRate(FIGURES.get(2))).contains(new BigDecimal("57.2"));
        assertThat(n.yearOverYearRate(FIGURES.get(1))).isEmpty();
    }

    @Test
    @DisplayName("직전 리포트가 없으면 신규 시그널이고 리비전은 비어 있다")
    void shouldBeNewWithoutPrevious() {
        EpsNotification n = notification(null, List.of(), null);

        assertThat(n.signal()).isEqualTo(RevisionSignal.NEW);
        assertThat(n.revisionRate()).isEmpty();
        assertThat(n.consensusGapRate()).isEmpty();
        assertThat(n.forwardPer()).isEmpty();
    }

    @Test
    @DisplayName("목표주가 0 은 없음으로 취급해 변화율을 만들지 않는다")
    void shouldTreatZeroTargetPriceAsAbsent() {
        EpsNotification zero = new EpsNotification(1L, "192080", "더블유게임즈", "키움증권", PUBLISHED,
                "Buy", 0L, FIGURES,
                new EpsNotification.PreviousReport(0L, PUBLISHED.minusMonths(1), 72_000L, "Buy", List.of()),
                List.of(), null);
        EpsNotification fromZero = notification(
                new EpsNotification.PreviousReport(0L, PUBLISHED.minusMonths(1), 0L, "Buy", List.of()),
                List.of(), null);

        assertThat(zero.hasTargetPrice()).isFalse();
        assertThat(zero.targetPriceChangeRate()).isEmpty();
        assertThat(fromZero.previousTargetPrice()).isEmpty();
        assertThat(fromZero.targetPriceChangeRate()).isEmpty();
    }

    @Test
    @DisplayName("추정치가 전혀 없으면 마지막 실적 연도를 기준으로 삼는다")
    void shouldFallBackKeyYear() {
        EpsNotification laterOnly = new EpsNotification(1L, "S", "C", "B", PUBLISHED, null, null,
                List.of(new EpsFigure(2025, false, BigDecimal.ONE), new EpsFigure(2027, true, BigDecimal.TEN)),
                null, List.of(), null);
        EpsNotification actualOnly = new EpsNotification(1L, "S", "C", "B", PUBLISHED, null, null,
                List.of(new EpsFigure(2024, false, BigDecimal.ONE), new EpsFigure(2025, false, BigDecimal.TEN)),
                null, List.of(), null);

        assertThat(laterOnly.keyYear()).isEqualTo(2027);
        assertThat(actualOnly.keyYear()).isEqualTo(2025);
    }

    @Test
    @DisplayName("리비전 시그널 구간: 10% 이상 강한 상향, 3% 이상 상향, ±3% 안 유지, -3% 이하 하향, -10% 이하 강한 하향")
    void shouldClassifySignal() {
        assertThat(RevisionSignal.of(new BigDecimal("10.0"))).isEqualTo(RevisionSignal.STRONG_UP);
        assertThat(RevisionSignal.of(new BigDecimal("3.0"))).isEqualTo(RevisionSignal.UP);
        assertThat(RevisionSignal.of(new BigDecimal("2.9"))).isEqualTo(RevisionSignal.FLAT);
        assertThat(RevisionSignal.of(new BigDecimal("-2.9"))).isEqualTo(RevisionSignal.FLAT);
        assertThat(RevisionSignal.of(new BigDecimal("-3.0"))).isEqualTo(RevisionSignal.DOWN);
        assertThat(RevisionSignal.of(new BigDecimal("-10.0"))).isEqualTo(RevisionSignal.STRONG_DOWN);
    }

    @Test
    @DisplayName("기준값이 음수면 절대값 대비 변화율, 0 이면 비어 있다")
    void shouldHandleNegativeAndZeroBase() {
        assertThat(EpsNotification.changeRate(new BigDecimal("-50"), new BigDecimal("-100")))
                .contains(new BigDecimal("50.0"));
        assertThat(EpsNotification.changeRate(new BigDecimal("50"), BigDecimal.ZERO)).isEmpty();
    }

    private static EpsNotification notification(EpsNotification.PreviousReport previous,
                                                List<EpsConsensus> consensus, Integer closePrice) {
        return new EpsNotification(650363L, "192080", "더블유게임즈", "키움증권", PUBLISHED,
                "Buy", 82_000L, FIGURES, previous, consensus, closePrice);
    }
}
