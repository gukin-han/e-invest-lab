package dev.gukin.einvestlab.research.infrastructure.slack;

import dev.gukin.einvestlab.global.config.SlackApiProperties;
import dev.gukin.einvestlab.research.domain.EpsExtractedEvent;
import dev.gukin.einvestlab.research.domain.EpsFigure;
import dev.gukin.einvestlab.research.domain.EpsNotificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("슬랙 EPS 알림 어댑터 단위 테스트")
class SlackEpsNotifierUnitTest {

    private final EpsExtractedEvent event = new EpsExtractedEvent(422780L, "016360", "삼성증권", List.of(
            new EpsFigure(2026, true, new BigDecimal("4087.5")),
            new EpsFigure(2025, false, new BigDecimal("2130"))));

    @Test
    @DisplayName("종목·리포트 식별자와 연도순 EPS(실적 A / 추정 E)를 한 메시지로 조립한다")
    void shouldBuildMessage() {
        assertThat(SlackEpsNotifierAdapter.buildMessage(event)).isEqualTo("""
                *EPS 추출* 삼성증권 (016360) — report_idx=422780
                - 2025A: 2,130
                - 2026E: 4,087.5""");
    }

    @Test
    @DisplayName("웹훅 URL 이 비어 있으면 요청 없이 알림 예외를 던진다")
    void shouldRejectWhenWebhookUrlMissing() {
        SlackEpsNotifierAdapter adapter = new SlackEpsNotifierAdapter(
                null, new ObjectMapper(), new SlackApiProperties(""));

        assertThatThrownBy(() -> adapter.notify(event))
                .isInstanceOf(EpsNotificationException.class)
                .hasMessageContaining("SLACK_WEBHOOK_URL");
    }
}
