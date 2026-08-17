package dev.gukin.einvestlab.support.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    static final List<Duration> BACKOFF = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(30),
            Duration.ofHours(2),
            Duration.ofHours(12));
    static final int MAX_ATTEMPTS = BACKOFF.size() + 1;
    private static final int LAST_ERROR_MAX_LENGTH = 500;

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(length = 16)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 50)
    private String aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(length = 500)
    private String lastError;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    public static OutboxEvent pending(UUID id, String eventType, String aggregateId,
                                      String payload, Instant now) {
        OutboxEvent event = new OutboxEvent();
        event.id = id;
        event.eventType = eventType;
        event.aggregateId = aggregateId;
        event.payload = payload;
        event.status = OutboxEventStatus.PENDING;
        event.attemptCount = 0;
        event.nextAttemptAt = now;
        event.createdAt = now;
        return event;
    }

    public void markSent(Instant now) {
        attemptCount++;
        status = OutboxEventStatus.SENT;
        sentAt = now;
        lastError = null;
    }

    public void markFailed(String error, Instant now) {
        attemptCount++;
        lastError = truncate(error);
        if (attemptCount >= MAX_ATTEMPTS) {
            status = OutboxEventStatus.DEAD;
            return;
        }
        nextAttemptAt = now.plus(BACKOFF.get(attemptCount - 1));
    }

    public void markDead(String error) {
        attemptCount++;
        lastError = truncate(error);
        status = OutboxEventStatus.DEAD;
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= LAST_ERROR_MAX_LENGTH ? error : error.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}
