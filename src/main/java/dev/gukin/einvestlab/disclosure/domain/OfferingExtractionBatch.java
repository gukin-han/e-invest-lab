package dev.gukin.einvestlab.disclosure.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offering_extraction_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfferingExtractionBatch {

    public enum Status {SUBMITTED, COLLECTED, FAILED}

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(length = 16)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String providerBatchId;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(nullable = false)
    private int requestCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(nullable = false)
    private Instant submittedAt;

    private Instant collectedAt;

    @Builder
    public OfferingExtractionBatch(UUID id, String providerBatchId, String model,
                                   int requestCount, Status status, Instant submittedAt) {
        this.id = id;
        this.providerBatchId = providerBatchId;
        this.model = model;
        this.requestCount = requestCount;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public void markCollected(Instant collectedAt) {
        this.status = Status.COLLECTED;
        this.collectedAt = collectedAt;
    }

    public void markFailed(Instant collectedAt) {
        this.status = Status.FAILED;
        this.collectedAt = collectedAt;
    }
}
