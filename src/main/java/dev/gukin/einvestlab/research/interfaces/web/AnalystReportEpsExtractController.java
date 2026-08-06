package dev.gukin.einvestlab.research.interfaces.web;

import dev.gukin.einvestlab.global.web.ApiResponse;
import dev.gukin.einvestlab.research.application.AnalystReportEpsExtractResult;
import dev.gukin.einvestlab.research.application.AnalystReportEpsExtractUseCase;
import dev.gukin.einvestlab.research.interfaces.web.dto.AnalystReportEpsExtractResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequiredArgsConstructor
public class AnalystReportEpsExtractController {

    private final AnalystReportEpsExtractUseCase extractUseCase;
    private final Clock clock;

    @PostMapping("/internal/analyst-reports/extract-eps")
    public ApiResponse<AnalystReportEpsExtractResponse> extractEps() {
        AnalystReportEpsExtractResult result = extractUseCase.extractAll(clock.instant());
        return ApiResponse.of(AnalystReportEpsExtractResponse.from(result));
    }
}
