package dev.gukin.einvestlab.market.infrastructure.persistence;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.ShareCountTrend;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
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
                listed_share_count,
                market_cap,
                collected_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                market_category = VALUES(market_category),
                open_price = VALUES(open_price),
                high_price = VALUES(high_price),
                low_price = VALUES(low_price),
                close_price = VALUES(close_price),
                volume = VALUES(volume),
                listed_share_count = VALUES(listed_share_count),
                market_cap = VALUES(market_cap),
                collected_at = VALUES(collected_at)
            """;

    private static final String SHARE_COUNT_TREND_SQL = """
            WITH seq AS (
                SELECT stock_code, trade_date, listed_share_count lsc,
                       LAG(listed_share_count)
                           OVER (PARTITION BY stock_code ORDER BY trade_date) prev
                FROM daily_stock_prices
                WHERE trade_date >= ? AND listed_share_count IS NOT NULL
            ),
            agg AS (
                SELECT stock_code,
                       SUM(CASE WHEN lsc < prev THEN prev - lsc ELSE 0 END) decreased_shares,
                       SUM(CASE WHEN lsc < prev THEN 1 ELSE 0 END) decrease_events,
                       MAX(CASE WHEN lsc < prev THEN trade_date END) last_decrease_date,
                       MAX(CASE WHEN lsc < prev THEN (prev - lsc) / prev * 100 ELSE 0 END) max_drop_pct
                FROM seq
                WHERE prev IS NOT NULL
                GROUP BY stock_code
            ),
            g AS (
                SELECT stock_code, MIN(trade_date) first_date, MAX(trade_date) last_trade_date
                FROM daily_stock_prices
                WHERE trade_date >= ? AND listed_share_count IS NOT NULL
                GROUP BY stock_code
            )
            SELECT g.stock_code, c.name company_name,
                   s.listed_share_count start_count,
                   e.listed_share_count end_count,
                   ROUND((e.listed_share_count - s.listed_share_count)
                         / s.listed_share_count * 100, 2) net_change_pct,
                   COALESCE(a.decrease_events, 0) decrease_events,
                   COALESCE(a.decreased_shares, 0) decreased_shares,
                   a.last_decrease_date,
                   ROUND(COALESCE(a.max_drop_pct, 0), 2) max_drop_pct,
                   e.market_cap
            FROM g
            JOIN daily_stock_prices s ON s.stock_code = g.stock_code AND s.trade_date = g.first_date
            JOIN daily_stock_prices e ON e.stock_code = g.stock_code AND e.trade_date = g.last_trade_date
            LEFT JOIN agg a ON a.stock_code = g.stock_code
            LEFT JOIN companies c ON c.stock_code = g.stock_code
            WHERE s.listed_share_count > 0 AND g.last_trade_date >= ?
              AND (? IS NULL OR COALESCE(a.max_drop_pct, 0) <= ?)
            ORDER BY net_change_pct %s, g.stock_code
            LIMIT ?
            """;

    private final JdbcTemplate jdbc;

    public List<ShareCountTrend> findShareCountTrends(LocalDate since, LocalDate listedCutoff,
                                                      boolean decreasing, BigDecimal maxSingleDropPct,
                                                      int limit) {
        String sql = SHARE_COUNT_TREND_SQL.formatted(decreasing ? "ASC" : "DESC");
        return jdbc.query(sql, (rs, rowNum) -> new ShareCountTrend(
                        rs.getString("stock_code"),
                        rs.getString("company_name"),
                        rs.getLong("start_count"),
                        rs.getLong("end_count"),
                        rs.getBigDecimal("net_change_pct"),
                        rs.getInt("decrease_events"),
                        rs.getLong("decreased_shares"),
                        rs.getObject("last_decrease_date", LocalDate.class),
                        rs.getBigDecimal("max_drop_pct"),
                        rs.getObject("market_cap", Long.class)),
                Date.valueOf(since), Date.valueOf(since), Date.valueOf(listedCutoff),
                maxSingleDropPct, maxSingleDropPct, limit);
    }

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
            statement.setObject(10, price.getListedShareCount(), Types.BIGINT);
            statement.setObject(11, price.getMarketCap(), Types.BIGINT);
            statement.setTimestamp(12, Timestamp.from(price.getCollectedAt()));
        });
        return prices.size();
    }
}
