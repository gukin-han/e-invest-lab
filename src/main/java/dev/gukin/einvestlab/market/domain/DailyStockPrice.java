package dev.gukin.einvestlab.market.domain;

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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_stock_prices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyStockPrice {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(length = 16)
    private UUID id;

    @Column(nullable = false, length = 6)
    private String stockCode;

    @Column(nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false, length = 20)
    private String marketCategory;

    @Column(nullable = false)
    private int openPrice;

    @Column(nullable = false)
    private int highPrice;

    @Column(nullable = false)
    private int lowPrice;

    @Column(nullable = false)
    private int closePrice;

    @Column(nullable = false)
    private long volume;

    @Column(nullable = false)
    private Instant collectedAt;

    @Builder
    public DailyStockPrice(UUID id, String stockCode, LocalDate tradeDate, String marketCategory,
                           int openPrice, int highPrice, int lowPrice, int closePrice,
                           long volume, Instant collectedAt) {
        this.id = id;
        this.stockCode = stockCode;
        this.tradeDate = tradeDate;
        this.marketCategory = marketCategory;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.collectedAt = collectedAt;
    }
}
