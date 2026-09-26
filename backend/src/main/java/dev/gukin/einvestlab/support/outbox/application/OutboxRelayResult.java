package dev.gukin.einvestlab.support.outbox.application;

public record OutboxRelayResult(int sent, int failed, int dead) {
}
