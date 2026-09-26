package dev.gukin.einvestlab.research.interfaces.web;

import dev.gukin.einvestlab.global.web.ApiResponse;
import dev.gukin.einvestlab.research.application.StockCoverageQuery;
import dev.gukin.einvestlab.research.interfaces.web.dto.CoveredStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StockCoverageController {

    private final StockCoverageQuery coverageQuery;
    private final Clock clock;

    @GetMapping("/api/stocks/recently-covered")
    public ApiResponse<List<CoveredStockResponse>> recentlyCovered(
            @RequestParam(required = false) Integer days) {
        return ApiResponse.of(coverageQuery.recentlyCovered(days, clock.instant()).stream()
                .map(CoveredStockResponse::from)
                .toList());
    }
}
