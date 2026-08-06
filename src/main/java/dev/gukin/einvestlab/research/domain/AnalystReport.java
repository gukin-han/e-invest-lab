package dev.gukin.einvestlab.research.domain;

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
@Table(name = "analyst_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalystReport {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(length = 16)
    private UUID id;

    @Column(nullable = false, unique = true)
    private long reportIdx;

    @Column(nullable = false, length = 6)
    private String stockCode;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 100)
    private String broker;

    private String authors;

    @Column(nullable = false)
    private LocalDate publishedDate;

    private Long targetPrice;

    @Column(length = 50)
    private String opinion;

    @Column(nullable = false)
    private Instant collectedAt;

    @Column(length = 300)
    private String pdfPath;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EpsExtractionStatus epsExtractionStatus;

    @Builder
    public AnalystReport(UUID id, long reportIdx, String stockCode, String companyName,
                         String title, String broker, String authors, LocalDate publishedDate,
                         Long targetPrice, String opinion, Instant collectedAt) {
        this.id = id;
        this.reportIdx = reportIdx;
        this.stockCode = stockCode;
        this.companyName = companyName;
        this.title = title;
        this.broker = broker;
        this.authors = authors;
        this.publishedDate = publishedDate;
        this.targetPrice = targetPrice;
        this.opinion = opinion;
        this.collectedAt = collectedAt;
    }

    public void attachPdf(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public void recordEpsExtraction(EpsExtractionStatus status) {
        this.epsExtractionStatus = status;
    }
}
