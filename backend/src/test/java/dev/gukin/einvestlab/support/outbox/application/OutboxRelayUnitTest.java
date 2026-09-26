package dev.gukin.einvestlab.support.outbox.application;

import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventHandler;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventRepository;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventStatus;
import dev.gukin.einvestlab.testsupport.RecordingTransactionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("아웃박스 릴레이 단위 테스트")
class OutboxRelayUnitTest {

    private static final Instant NOW = Instant.parse("2026-08-17T09:00:00Z");

    private final StubRepository repository = new StubRepository();
    private final RecordingHandler handler = new RecordingHandler("EPS_EXTRACTED");
    private final OutboxRelayUseCase useCase = new OutboxRelayUseCase(
            repository, List.of(handler), new RecordingTransactionManager());

    @Test
    @DisplayName("핸들러가 성공하면 SENT 로 마킹하고 전송 시각을 남긴다")
    void shouldMarkSentOnSuccess() {
        OutboxEvent event = pending("EPS_EXTRACTED");
        repository.due.add(event);

        OutboxRelayResult result = useCase.relay(NOW);

        assertThat(result).isEqualTo(new OutboxRelayResult(1, 0, 0));
        assertThat(handler.handled).containsExactly(event);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(event.getSentAt()).isEqualTo(NOW);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(repository.saved).containsExactly(event);
    }

    @Test
    @DisplayName("핸들러가 실패하면 PENDING 을 유지하고 백오프만큼 다음 시도를 미룬다")
    void shouldBackoffOnFailure() {
        OutboxEvent event = pending("EPS_EXTRACTED");
        repository.due.add(event);
        handler.failWith = new IllegalStateException("slack down");

        OutboxRelayResult result = useCase.relay(NOW);

        assertThat(result).isEqualTo(new OutboxRelayResult(0, 1, 0));
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
        assertThat(event.getLastError()).contains("slack down");
        assertThat(repository.saved).containsExactly(event);
    }

    @Test
    @DisplayName("실패가 상한에 닿으면 DEAD 로 마킹해 더 이상 폴링되지 않게 한다")
    void shouldMarkDeadAfterMaxAttempts() {
        OutboxEvent event = pending("EPS_EXTRACTED");
        handler.failWith = new IllegalStateException("still down");
        Instant at = NOW;
        for (int i = 0; i < 6; i++) {
            repository.due = new ArrayList<>(List.of(event));
            useCase.relay(at);
            at = event.getNextAttemptAt();
        }

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD);
        assertThat(event.getAttemptCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("한 건 실패가 나머지 건의 전송을 막지 않는다")
    void shouldIsolateFailurePerEvent() {
        OutboxEvent failing = pending("EPS_EXTRACTED");
        OutboxEvent healthy = pending("EPS_EXTRACTED");
        repository.due.addAll(List.of(failing, healthy));
        handler.failOn = failing;

        OutboxRelayResult result = useCase.relay(NOW);

        assertThat(result).isEqualTo(new OutboxRelayResult(1, 1, 0));
        assertThat(failing.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(healthy.getStatus()).isEqualTo(OutboxEventStatus.SENT);
    }

    @Test
    @DisplayName("지원 핸들러가 없는 타입은 재시도 없이 DEAD 로 남긴다")
    void shouldMarkDeadWhenNoHandler() {
        OutboxEvent event = pending("UNKNOWN");
        repository.due.add(event);

        OutboxRelayResult result = useCase.relay(NOW);

        assertThat(result).isEqualTo(new OutboxRelayResult(0, 0, 1));
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD);
        assertThat(event.getLastError()).contains("UNKNOWN");
        assertThat(handler.handled).isEmpty();
    }

    private static OutboxEvent pending(String type) {
        return OutboxEvent.pending(UUID.randomUUID(), type, "1", "{}", NOW.minus(Duration.ofMinutes(5)));
    }

    private static class StubRepository implements OutboxEventRepository {
        List<OutboxEvent> due = new ArrayList<>();
        final List<OutboxEvent> saved = new ArrayList<>();

        @Override
        public OutboxEvent save(OutboxEvent event) {
            saved.add(event);
            return event;
        }

        @Override
        public List<OutboxEvent> findDue(Instant now, int limit) {
            return due;
        }
    }

    private static class RecordingHandler implements OutboxEventHandler {
        private final String type;
        final List<OutboxEvent> handled = new ArrayList<>();
        RuntimeException failWith;
        OutboxEvent failOn;

        RecordingHandler(String type) {
            this.type = type;
        }

        @Override
        public boolean supports(String eventType) {
            return type.equals(eventType);
        }

        @Override
        public void handle(OutboxEvent event) {
            if (failWith != null || event == failOn) {
                throw failWith != null ? failWith : new IllegalStateException("fail");
            }
            handled.add(event);
        }
    }
}
