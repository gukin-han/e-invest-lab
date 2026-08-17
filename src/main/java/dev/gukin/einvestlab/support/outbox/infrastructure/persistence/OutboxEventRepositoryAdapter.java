package dev.gukin.einvestlab.support.outbox.infrastructure.persistence;

import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final OutboxEventJpaRepository jpa;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return jpa.save(event);
    }
}
