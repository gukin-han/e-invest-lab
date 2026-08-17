package dev.gukin.einvestlab.support.outbox.infrastructure.persistence;

import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, UUID> {
}
