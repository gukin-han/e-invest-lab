package dev.gukin.einvestlab.support.outbox.infrastructure.persistence;

import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventRepository;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final OutboxEventJpaRepository jpa;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return jpa.save(event);
    }

    @Override
    public List<OutboxEvent> findDue(Instant now, int limit) {
        return jpa.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                OutboxEventStatus.PENDING, now, PageRequest.of(0, limit));
    }
}
