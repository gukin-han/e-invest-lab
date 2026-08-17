package dev.gukin.einvestlab.support.outbox.domain;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);
}
