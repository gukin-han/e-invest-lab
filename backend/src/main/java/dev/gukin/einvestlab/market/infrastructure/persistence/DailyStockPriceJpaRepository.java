package dev.gukin.einvestlab.market.infrastructure.persistence;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyStockPriceJpaRepository extends JpaRepository<DailyStockPrice, UUID> {

    Optional<DailyStockPrice> findTopByStockCodeOrderByTradeDateDesc(String stockCode);

    boolean existsByTradeDate(LocalDate tradeDate);

    List<DailyStockPrice> findAllByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(
            String stockCode, LocalDate from, LocalDate to);
}
