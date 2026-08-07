package dev.gukin.einvestlab.market.infrastructure.persistence;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DailyStockPriceJpaRepository extends JpaRepository<DailyStockPrice, UUID> {

    Optional<DailyStockPrice> findTopByStockCodeOrderByTradeDateDesc(String stockCode);
}
