package dev.gukin.einvestlab.research.interfaces.web;

import dev.gukin.einvestlab.global.web.ApiResponse;
import dev.gukin.einvestlab.research.application.AnalystReportPdfDownloadResult;
import dev.gukin.einvestlab.research.application.AnalystReportPdfDownloadUseCase;
import dev.gukin.einvestlab.research.interfaces.web.dto.AnalystReportPdfDownloadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnalystReportPdfDownloadController {

    private final AnalystReportPdfDownloadUseCase downloadUseCase;

    @PostMapping("/internal/analyst-reports/download-pdfs")
    public ApiResponse<AnalystReportPdfDownloadResponse> downloadPdfs() {
        AnalystReportPdfDownloadResult result = downloadUseCase.downloadAll();
        return ApiResponse.of(AnalystReportPdfDownloadResponse.from(result));
    }
}
