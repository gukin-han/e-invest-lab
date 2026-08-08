package dev.gukin.einvestlab.disclosure.domain;

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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "offerings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Offering {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(length = 16)
    private UUID id;

    @Column(nullable = false, length = 8)
    private String corpCode;

    @Column(nullable = false, length = 14)
    private String filingNumber;

    @Column(length = 50)
    private String businessPart;

    @Column(length = 100)
    private String segment;

    @Column(length = 100)
    private String qualifier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> products;

    @Column(precision = 20, scale = 2)
    private BigDecimal revenueAmount;

    @Column(length = 30)
    private String revenueUnit;

    @Column(length = 50)
    private String revenueBasis;

    @Column(precision = 6, scale = 2)
    private BigDecimal revenueShare;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> customers;

    @Column(length = 100)
    private String entityName;

    private Integer fiscalYear;

    @Column(nullable = false)
    private Instant extractedAt;

    @Builder
    public Offering(UUID id, String corpCode, String filingNumber, String businessPart,
                    String segment, String qualifier, List<String> products,
                    BigDecimal revenueAmount, String revenueUnit, String revenueBasis,
                    BigDecimal revenueShare, List<String> customers, String entityName,
                    Integer fiscalYear, Instant extractedAt) {
        this.id = id;
        this.corpCode = corpCode;
        this.filingNumber = filingNumber;
        this.businessPart = businessPart;
        this.segment = segment;
        this.qualifier = qualifier;
        this.products = products;
        this.revenueAmount = revenueAmount;
        this.revenueUnit = revenueUnit;
        this.revenueBasis = revenueBasis;
        this.revenueShare = revenueShare;
        this.customers = customers;
        this.entityName = entityName;
        this.fiscalYear = fiscalYear;
        this.extractedAt = extractedAt;
    }
}
