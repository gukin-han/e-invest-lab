package dev.gukin.einvestlab.research.interfaces.scheduler;

import dev.gukin.einvestlab.research.application.AnalystReportCollectResult;
import dev.gukin.einvestlab.research.application.AnalystReportCollectUseCase;
import dev.gukin.einvestlab.research.application.AnalystReportEpsExtractResult;
import dev.gukin.einvestlab.research.application.AnalystReportEpsExtractUseCase;
import dev.gukin.einvestlab.research.application.AnalystReportPdfDownloadResult;
import dev.gukin.einvestlab.research.application.AnalystReportPdfDownloadUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalystReportScheduler {

    private final AnalystReportCollectUseCase collectUseCase;
    private final AnalystReportPdfDownloadUseCase downloadUseCase;
    private final AnalystReportEpsExtractUseCase extractUseCase;
    private final Clock clock;

    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
    void pollDaily() {
        Instant baseTime = clock.instant();
        collect(baseTime);
        downloadPdfs();
        extractEps(baseTime);
    }

    private void collect(Instant baseTime) {
        try {
            log.info("analyst report collect started.");
            AnalystReportCollectResult result = collectUseCase.collect(null, null, baseTime);
            log.info("analyst report collect completed. collected={} skipped={}",
                    result.collected(), result.skipped());
        } catch (Exception e) {
            log.error("analyst report collect failed.", e);
        }
    }

    private void downloadPdfs() {
        try {
            log.info("analyst report pdf download started.");
            AnalystReportPdfDownloadResult result = downloadUseCase.downloadAll();
            log.info("analyst report pdf download completed. downloaded={} failed={}",
                    result.downloaded(), result.failed());
        } catch (Exception e) {
            log.error("analyst report pdf download failed.", e);
        }
    }

    private void extractEps(Instant baseTime) {
        try {
            log.info("analyst report eps extract started.");
            AnalystReportEpsExtractResult result = extractUseCase.extractAll(baseTime);
            log.info("analyst report eps extract completed. extracted={} noSummaryTable={} failed={}",
                    result.extracted(), result.noSummaryTable(), result.failed());
        } catch (Exception e) {
            log.error("analyst report eps extract failed.", e);
        }
    }
}
