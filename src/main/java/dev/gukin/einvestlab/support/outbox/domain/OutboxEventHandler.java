package dev.gukin.einvestlab.support.outbox.domain;

public interface OutboxEventHandler {

    boolean supports(String eventType);

    void handle(OutboxEvent event);
}
