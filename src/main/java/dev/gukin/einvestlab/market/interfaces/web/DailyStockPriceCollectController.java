package dev.gukin.einvestlab.market.interfaces.web;

import dev.gukin.einvestlab.global.web.ApiResponse;
import dev.gukin.einvestlab.market.application.DailyStockPriceCollectResult;
import dev.gukin.einvestlab.market.application.DailyStockPriceCollectUseCase;
import dev.gukin.einvestlab.market.interfaces.web.dto.DailyStockPriceCollectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class DailyStockPriceCollectController {

    private final DailyStockPriceCollectUseCase collectUseCase;
    private final Clock clock;

    @PostMapping("/internal/daily-stock-prices/collect")
    public ApiResponse<DailyStockPriceCollectResponse> collect(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DailyStockPriceCollectResult result = collectUseCase.collect(from, to, clock.instant());
        return ApiResponse.of(DailyStockPriceCollectResponse.from(result));
    }
}
