package dev.gukin.einvestlab.market.infrastructure.persistence;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DailyStockPriceJdbcRepository {

    private static final String UPSERT_PRICES_SQL = """
            INSERT INTO daily_stock_prices (
                id,
                stock_code,
                trade_date,
                market_category,
                open_price,
                high_price,
                low_price,
                close_price,
                volume,
                collected_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                market_category = VALUES(market_category),
                open_price = VALUES(open_price),
                high_price = VALUES(high_price),
                low_price = VALUES(low_price),
                close_price = VALUES(close_price),
                volume = VALUES(volume),
                collected_at = VALUES(collected_at)
            """;

    private final JdbcTemplate jdbc;

    public int upsertPrices(List<DailyStockPrice> prices) {
        jdbc.batchUpdate(UPSERT_PRICES_SQL, prices, prices.size(), (statement, price) -> {
            statement.setBytes(1, Ids.toBytes(price.getId()));
            statement.setString(2, price.getStockCode());
            statement.setDate(3, Date.valueOf(price.getTradeDate()));
            statement.setString(4, price.getMarketCategory());
            statement.setInt(5, price.getOpenPrice());
            statement.setInt(6, price.getHighPrice());
            statement.setInt(7, price.getLowPrice());
            statement.setInt(8, price.getClosePrice());
            statement.setLong(9, price.getVolume());
            statement.setTimestamp(10, Timestamp.from(price.getCollectedAt()));
        });
        return prices.size();
    }
}
