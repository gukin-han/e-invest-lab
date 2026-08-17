package dev.gukin.einvestlab.research.interfaces.web;

import dev.gukin.einvestlab.global.web.ApiResponse;
import dev.gukin.einvestlab.research.application.AnalystReportPdfPurgeResult;
import dev.gukin.einvestlab.research.application.AnalystReportPdfPurgeUseCase;
import dev.gukin.einvestlab.research.interfaces.web.dto.AnalystReportPdfPurgeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequiredArgsConstructor
public class AnalystReportPdfPurgeController {

    private final AnalystReportPdfPurgeUseCase purgeUseCase;
    private final Clock clock;

    @PostMapping("/internal/analyst-reports/purge-pdfs")
    public ApiResponse<AnalystReportPdfPurgeResponse> purgePdfs() {
        AnalystReportPdfPurgeResult result = purgeUseCase.purgeAll(clock.instant());
        return ApiResponse.of(AnalystReportPdfPurgeResponse.from(result));
    }
}
