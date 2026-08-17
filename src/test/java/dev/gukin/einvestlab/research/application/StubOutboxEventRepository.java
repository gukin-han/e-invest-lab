package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventRepository;

import java.util.ArrayList;
import java.util.List;

class StubOutboxEventRepository implements OutboxEventRepository {

    final List<OutboxEvent> saved = new ArrayList<>();

    @Override
    public OutboxEvent save(OutboxEvent event) {
        saved.add(event);
        return event;
    }
}
