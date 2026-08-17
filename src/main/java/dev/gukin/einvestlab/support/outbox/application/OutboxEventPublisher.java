package dev.gukin.einvestlab.support.outbox.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, String aggregateId, Object payload, Instant now) {
        repository.save(OutboxEvent.pending(
                Ids.generate(), eventType, aggregateId,
                objectMapper.writeValueAsString(payload),
                now)
        );
    }
}
