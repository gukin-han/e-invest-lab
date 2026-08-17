package dev.gukin.einvestlab.support.outbox.domain;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findDue(Instant now, int limit);
}
