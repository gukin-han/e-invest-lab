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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "business_contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessContent {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(length = 16)
    private UUID id;

    @Column(nullable = false, length = 8)
    private String corpCode;

    @Column(nullable = false, unique = true, length = 14)
    private String filingNumber;

    @Column(nullable = false)
    private LocalDate filedDate;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(nullable = false)
    private Instant collectedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OfferingExtractionStatus offeringExtractionStatus;

    @Column(columnDefinition = "TEXT")
    private String offeringExtractionNote;

    @JdbcTypeCode(SqlTypes.JSON)
    private String offeringExtractionDrafts;

    @Builder
    public BusinessContent(UUID id, String corpCode, String filingNumber,
                           LocalDate filedDate, String content, Instant collectedAt) {
        this.id = id;
        this.corpCode = corpCode;
        this.filingNumber = filingNumber;
        this.filedDate = filedDate;
        this.content = content;
        this.collectedAt = collectedAt;
    }

    public void recordOfferingExtraction(OfferingExtractionStatus status, String note, String drafts) {
        this.offeringExtractionStatus = status;
        this.offeringExtractionNote = note;
        this.offeringExtractionDrafts = drafts;
    }
}
