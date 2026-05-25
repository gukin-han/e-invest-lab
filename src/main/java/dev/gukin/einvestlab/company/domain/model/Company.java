package dev.gukin.einvestlab.company.domain.model;

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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(length = 16)
    private UUID id;

    @Column(nullable = false, unique = true, length = 8)
    private String corpCode;

    @Column(nullable = false)
    private String name;

    private String englishName;

    @Column(length = 6)
    private String stockCode;

    @Column(nullable = false)
    private LocalDate masterModifiedDate;

    @Builder
    public Company(String corpCode, String name, String englishName,
                   String stockCode, LocalDate masterModifiedDate) {
        this.corpCode = corpCode;
        this.name = name;
        this.englishName = englishName;
        this.stockCode = stockCode;
        this.masterModifiedDate = masterModifiedDate;
    }
}
