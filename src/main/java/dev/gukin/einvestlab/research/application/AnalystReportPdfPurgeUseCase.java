package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfStore;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalystReportPdfPurgeUseCase {

    static final Period RETENTION = Period.ofYears(1);
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final AnalystReportRepository repository;
    private final AnalystReportPdfStore pdfStore;

    public AnalystReportPdfPurgeResult purgeAll(Instant baseTime) {
        LocalDate cutoff = LocalDate.ofInstant(baseTime, KOREA).minus(RETENTION);
        int purged = 0;
        int failed = 0;
        for (AnalystReport report : repository.findAllWithPdfPublishedBefore(cutoff)) {
            try {
                pdfStore.delete(report.getPdfPath());
            } catch (UncheckedIOException e) {
                log.warn("PDF 삭제 실패 (report_idx={}, path={}): {}",
                        report.getReportIdx(), report.getPdfPath(), e.getMessage());
                failed++;
                continue;
            }
            report.purgePdf(baseTime);
            repository.save(report);
            purged++;
        }
        return new AnalystReportPdfPurgeResult(purged, failed);
    }
}
