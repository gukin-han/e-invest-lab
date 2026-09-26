package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfSource;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfStore;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import dev.gukin.einvestlab.research.domain.ResearchSourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalystReportPdfDownloadUseCase {

    private final AnalystReportRepository repository;
    private final AnalystReportPdfSource pdfSource;
    private final AnalystReportPdfStore pdfStore;

    public AnalystReportPdfDownloadResult downloadAll() {
        int downloaded = 0;
        int failed = 0;
        for (AnalystReport report : repository.findAllWithoutPdf()) {
            try {
                byte[] pdf = pdfSource.fetchPdf(report.getReportIdx());
                String path = pdfStore.store(report.getReportIdx(), report.getPublishedDate(), pdf);
                report.attachPdf(path);
                repository.save(report);
                downloaded++;
            } catch (ResearchSourceException e) {
                log.warn("PDF 다운로드 실패 (report_idx={}): {}", report.getReportIdx(), e.getMessage());
                failed++;
            }
        }
        return new AnalystReportPdfDownloadResult(downloaded, failed);
    }
}
