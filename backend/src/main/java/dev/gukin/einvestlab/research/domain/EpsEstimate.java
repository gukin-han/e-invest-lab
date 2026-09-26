package dev.gukin.einvestlab.research.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eps_estimates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EpsEstimate {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(length = 16)
    private UUID id;

    @Column(nullable = false)
    private long reportIdx;

    @Column(nullable = false)
    private int fiscalYear;

    @Column(nullable = false)
    private boolean estimated;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal eps;

    @Column(nullable = false)
    private Instant extractedAt;

    @Builder
    public EpsEstimate(UUID id, long reportIdx, int fiscalYear,
                       boolean estimated, BigDecimal eps, Instant extractedAt) {
        this.id = id;
        this.reportIdx = reportIdx;
        this.fiscalYear = fiscalYear;
        this.estimated = estimated;
        this.eps = eps;
        this.extractedAt = extractedAt;
    }
}
