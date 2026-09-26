package dev.gukin.einvestlab.market.interfaces.web;

import dev.gukin.einvestlab.global.web.ApiResponse;
import dev.gukin.einvestlab.market.application.ShareCountTrendQuery;
import dev.gukin.einvestlab.market.application.StockPriceQuery;
import dev.gukin.einvestlab.market.interfaces.web.dto.ShareCountTrendResponse;
import dev.gukin.einvestlab.market.interfaces.web.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StockPriceController {

    private final StockPriceQuery priceQuery;
    private final ShareCountTrendQuery trendQuery;
    private final Clock clock;

    @GetMapping("/api/stocks/share-count-trends")
    public ApiResponse<List<ShareCountTrendResponse>> shareCountTrends(
            @RequestParam(defaultValue = "3") int years,
            @RequestParam(defaultValue = "decrease") String direction,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.of(trendQuery.rank(years, "decrease".equals(direction),
                        Math.min(limit, 200), clock.instant()).stream()
                .map(ShareCountTrendResponse::from)
                .toList());
    }

    @GetMapping("/api/stocks/{stockCode}/prices")
    public ApiResponse<List<StockPriceResponse>> prices(
            @PathVariable String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.of(priceQuery.series(stockCode, from, to, clock.instant()).stream()
                .map(StockPriceResponse::from)
                .toList());
    }
}
