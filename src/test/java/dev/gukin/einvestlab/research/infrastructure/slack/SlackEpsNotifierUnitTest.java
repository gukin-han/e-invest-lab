package dev.gukin.einvestlab.research.infrastructure.slack;

import dev.gukin.einvestlab.global.config.HankyungApiProperties;
import dev.gukin.einvestlab.global.config.SlackApiProperties;
import dev.gukin.einvestlab.research.domain.EpsConsensus;
import dev.gukin.einvestlab.research.domain.EpsFigure;
import dev.gukin.einvestlab.research.domain.EpsNotification;
import dev.gukin.einvestlab.research.domain.EpsNotificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("슬랙 EPS 알림 어댑터 단위 테스트")
class SlackEpsNotifierUnitTest {

    private static final String HANKYUNG = "https://consensus.hankyung.com";
    private static final LocalDate PUBLISHED = LocalDate.of(2026, 8, 17);
    private static final List<EpsFigure> FIGURES = List.of(
            new EpsFigure(2024, false, new BigDecimal("8708")),
            new EpsFigure(2025, false, new BigDecimal("6106")),
            new EpsFigure(2026, true, new BigDecimal("9599")),
            new EpsFigure(2027, true, new BigDecimal("11858")),
            new EpsFigure(2028, true, new BigDecimal("12792")));

    @Test
    @DisplayName("시그널 → 기준 연도 EPS(직전·컨센서스 들여쓰기) → 목표주가·의견 → 현재가·PER → EPS 경로 → 링크, 한 줄에 한 사실")
    void shouldRenderFullMessage() {
        EpsNotification n = new EpsNotification(650363L, "192080", "더블유게임즈", "키움증권", PUBLISHED,
                "Buy", 82_000L, FIGURES,
                new EpsNotification.PreviousReport(1L, PUBLISHED.minusMonths(1), 72_000L, "Buy",
                        List.of(new EpsFigure(2026, true, new BigDecimal("8850")))),
                List.of(new EpsConsensus(2026, new BigDecimal("9100"), 4, null, null)),
                new EpsNotification.LatestPrice(58_000, LocalDate.of(2026, 8, 14)));

        assertThat(new SlackEpsMessage(n, HANKYUNG).render()).isEqualTo("""
                *[상향] 더블유게임즈 (192080)* · 키움증권 · 8/17
                2026E EPS  9,599
                　　직전 8,850 → +8.5%
                　　컨센서스 9,100 대비 +5.5%
                목표주가  72,000 → 82,000 (+13.9%) · Buy
                현재가  58,000 (8/14) · PER 6.0배 (키움증권 2026E 기준)

                EPS 경로
                2024A  8,708
                2025A  6,106  -29.9%
                2026E  9,599  +57.2%
                2027E  11,858  +23.5%
                2028E  12,792  +7.9%
                리포트: https://consensus.hankyung.com/analysis/downpdf?report_idx=650363""");
    }

    @Test
    @DisplayName("직전 리포트·컨센서스·시세·목표주가가 없으면 해당 줄을 빼고 신규로 표기한다")
    void shouldRenderMinimalMessage() {
        EpsNotification n = new EpsNotification(1L, "016360", "삼성증권", "LS증권", PUBLISHED,
                "투자의견없음", 0L,
                List.of(new EpsFigure(2026, true, new BigDecimal("4087.5"))),
                null, List.of(), null);

        assertThat(new SlackEpsMessage(n, HANKYUNG).render()).isEqualTo("""
                *[신규] 삼성증권 (016360)* · LS증권 · 8/17
                2026E EPS  4,087.5
                투자의견  투자의견없음

                EPS 경로
                2026E  4,087.5
                리포트: https://consensus.hankyung.com/analysis/downpdf?report_idx=1""");
    }

    @Test
    @DisplayName("목표주가가 직전과 같으면 화살표 대신 유지로 줄인다")
    void shouldRenderUnchangedTargetPrice() {
        EpsNotification n = new EpsNotification(1L, "011200", "HMM", "LS증권", PUBLISHED, "Buy", 30_000L,
                List.of(new EpsFigure(2026, true, new BigDecimal("2235"))),
                new EpsNotification.PreviousReport(0L, PUBLISHED.minusDays(14), 30_000L, "Buy",
                        List.of(new EpsFigure(2026, true, new BigDecimal("2424")))),
                List.of(), null);

        assertThat(new SlackEpsMessage(n, HANKYUNG).render())
                .contains("*[하향] HMM (011200)*")
                .contains("직전 2,424 → -7.8%")
                .contains("목표주가  30,000 유지 · Buy")
                .doesNotContain("→ 30,000");
    }

    @Test
    @DisplayName("웹훅 URL 이 비어 있으면 요청 없이 알림 예외를 던진다")
    void shouldRejectWhenWebhookUrlMissing() {
        SlackEpsNotifierAdapter adapter = new SlackEpsNotifierAdapter(
                null, new ObjectMapper(), new SlackApiProperties(""), new HankyungApiProperties(HANKYUNG));
        EpsNotification n = new EpsNotification(1L, "S", "C", "B", PUBLISHED, null, null,
                List.of(new EpsFigure(2026, true, BigDecimal.ONE)), null, List.of(), null);

        assertThatThrownBy(() -> adapter.notify(n))
                .isInstanceOf(EpsNotificationException.class)
                .hasMessageContaining("SLACK_WEBHOOK_URL");
    }
}
