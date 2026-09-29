package dev.gukin.einvestlab.market.application;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.market.domain.PriceDistribution;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockPriceQuery {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_DAYS = 90;
    private static final int DISTRIBUTION_BINS = 24;

    private final DailyStockPriceRepository priceRepository;

    public List<DailyStockPrice> series(String stockCode, LocalDate from, LocalDate to, Instant baseTime) {
        LocalDate effectiveTo = to != null ? to : LocalDate.ofInstant(baseTime, KOREA);
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_DAYS);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("조회 기간 역전: " + effectiveFrom + " > " + effectiveTo);
        }
        return priceRepository.findSeries(stockCode, effectiveFrom, effectiveTo);
    }

    public Optional<PriceDistribution> distribution(String stockCode, LocalDate from, LocalDate to,
                                                    Instant baseTime) {
        return PriceDistribution.of(series(stockCode, from, to, baseTime), DISTRIBUTION_BINS);
    }
}
