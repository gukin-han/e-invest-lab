package dev.gukin.einvestlab.support.outbox.infrastructure;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventRepository;
import dev.gukin.einvestlab.support.outbox.infrastructure.persistence.OutboxEventJpaRepository;
import dev.gukin.einvestlab.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("아웃박스 이벤트 영속화 통합 테스트")
class OutboxEventPersistenceIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-17T09:00:00Z");

    @Autowired
    private OutboxEventRepository repository;

    @Autowired
    private OutboxEventJpaRepository jpa;

    @AfterEach
    void tearDown() {
        jpa.deleteAllInBatch();
    }

    @Test
    @DisplayName("PENDING 이고 시도 시각이 지난 행만 오래된 순으로 돌려준다")
    void shouldFindOnlyDuePendingEvents() {
        OutboxEvent overdue = repository.save(OutboxEvent.pending(
                Ids.generate(), "T", "1", "{\"a\":1}", NOW.minus(Duration.ofMinutes(10))));
        OutboxEvent dueNow = repository.save(OutboxEvent.pending(
                Ids.generate(), "T", "2", "{\"a\":2}", NOW));
        repository.save(OutboxEvent.pending(
                Ids.generate(), "T", "3", "{\"a\":3}", NOW.plus(Duration.ofMinutes(1))));
        OutboxEvent sent = OutboxEvent.pending(Ids.generate(), "T", "4", "{\"a\":4}", NOW.minus(Duration.ofHours(1)));
        sent.markSent(NOW);
        repository.save(sent);
        OutboxEvent dead = OutboxEvent.pending(Ids.generate(), "T", "5", "{\"a\":5}", NOW.minus(Duration.ofHours(1)));
        dead.markDead("x");
        repository.save(dead);

        assertThat(repository.findDue(NOW, 10))
                .extracting(OutboxEvent::getId)
                .containsExactly(overdue.getId(), dueNow.getId());
    }

    @Test
    @DisplayName("실패 마킹 후 재조회하면 다음 시도 시각과 마지막 오류가 유지된다")
    void shouldPersistFailureState() {
        OutboxEvent event = repository.save(OutboxEvent.pending(
                Ids.generate(), "T", "1", "{}", NOW));
        event.markFailed("boom", NOW);
        repository.save(event);

        OutboxEvent reloaded = jpa.findById(event.getId()).orElseThrow();
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(reloaded.getLastError()).isEqualTo("boom");
        assertThat(reloaded.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
        assertThat(repository.findDue(NOW, 10)).isEmpty();
        assertThat(repository.findDue(NOW.plus(Duration.ofMinutes(1)), 10)).hasSize(1);
    }
}
