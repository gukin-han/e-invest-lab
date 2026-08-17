package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.research.domain.EpsExtractedEvent;
import dev.gukin.einvestlab.research.domain.EpsNotifier;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEvent;
import dev.gukin.einvestlab.support.outbox.domain.OutboxEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class EpsExtractedNotifyHandler implements OutboxEventHandler {

    private final EpsNotifier notifier;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String eventType) {
        return EpsExtractedEvent.TYPE.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        notifier.notify(objectMapper.readValue(event.getPayload(), EpsExtractedEvent.class));
    }
}
