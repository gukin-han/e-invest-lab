package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.research.domain.EpsExtractedEvent;
import dev.gukin.einvestlab.research.domain.EpsFigure;
import dev.gukin.einvestlab.research.domain.EpsNotifier;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EPS 추출 이벤트 알림 핸들러 단위 테스트")
class EpsExtractedNotifyHandlerUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<EpsExtractedEvent> notified = new ArrayList<>();
    private final EpsExtractedNotifyHandler handler =
            new EpsExtractedNotifyHandler((EpsNotifier) notified::add, objectMapper);

    @Test
    @DisplayName("EPS_EXTRACTED 타입만 지원한다")
    void shouldSupportOnlyEpsExtracted() {
        assertThat(handler.supports("EPS_EXTRACTED")).isTrue();
        assertThat(handler.supports("OTHER")).isFalse();
    }

    @Test
    @DisplayName("페이로드를 이벤트로 되살려 알림 포트에 넘긴다")
    void shouldDeserializePayloadAndNotify() {
        EpsExtractedEvent event = new EpsExtractedEvent(1L, "016360", "삼성증권",
                List.of(new EpsFigure(2026, true, new BigDecimal("4087"))));
        OutboxEvent outbox = OutboxEvent.pending(UUID.randomUUID(), EpsExtractedEvent.TYPE, "1",
                objectMapper.writeValueAsString(event), Instant.EPOCH);

        handler.handle(outbox);

        assertThat(notified).containsExactly(event);
    }
}
