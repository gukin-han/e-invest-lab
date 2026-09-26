package dev.gukin.einvestlab.research.interfaces.web;

import dev.gukin.einvestlab.global.web.ApiResponse;
import dev.gukin.einvestlab.research.application.AnalystReportCollectResult;
import dev.gukin.einvestlab.research.application.AnalystReportCollectUseCase;
import dev.gukin.einvestlab.research.interfaces.web.dto.AnalystReportCollectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class AnalystReportCollectController {

    private final AnalystReportCollectUseCase collectUseCase;
    private final Clock clock;

    @PostMapping("/internal/analyst-reports/collect")
    public ApiResponse<AnalystReportCollectResponse> collect(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AnalystReportCollectResult result = collectUseCase.collect(from, to, clock.instant());
        return ApiResponse.of(AnalystReportCollectResponse.from(result));
    }
}
