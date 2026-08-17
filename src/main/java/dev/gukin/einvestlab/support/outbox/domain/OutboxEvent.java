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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

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
}
