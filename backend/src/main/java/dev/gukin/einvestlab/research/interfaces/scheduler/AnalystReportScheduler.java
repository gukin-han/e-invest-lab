package dev.gukin.einvestlab.research.interfaces.scheduler;

import dev.gukin.einvestlab.research.application.AnalystReportCollectResult;
import dev.gukin.einvestlab.research.application.AnalystReportCollectUseCase;
import dev.gukin.einvestlab.research.application.AnalystReportEpsExtractResult;
import dev.gukin.einvestlab.research.application.AnalystReportEpsExtractUseCase;
import dev.gukin.einvestlab.research.application.AnalystReportPdfDownloadResult;
import dev.gukin.einvestlab.research.application.AnalystReportPdfDownloadUseCase;
import dev.gukin.einvestlab.research.application.AnalystReportPdfPurgeResult;
import dev.gukin.einvestlab.research.application.AnalystReportPdfPurgeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalystReportScheduler {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final int FREQUENT_LOOKBACK_DAYS = 1;

    private final AnalystReportCollectUseCase collectUseCase;
    private final AnalystReportPdfDownloadUseCase downloadUseCase;
    private final AnalystReportEpsExtractUseCase extractUseCase;
    private final AnalystReportPdfPurgeUseCase purgeUseCase;
    private final Clock clock;

    @Scheduled(cron = "0 */10 7-15 * * MON-FRI", zone = "Asia/Seoul")
    void pollFrequently() {
        Instant baseTime = clock.instant();
        LocalDate today = LocalDate.ofInstant(baseTime, KOREA);
        collect(today.minusDays(FREQUENT_LOOKBACK_DAYS), today, baseTime, "frequent");
        downloadPdfs();
        extractEps(baseTime);
    }

    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
    void pollDaily() {
        Instant baseTime = clock.instant();
        collect(null, null, baseTime, "daily");
        downloadPdfs();
        extractEps(baseTime);
        purgePdfs(baseTime);
    }

    private void collect(LocalDate from, LocalDate to, Instant baseTime, String mode) {
        try {
            AnalystReportCollectResult result = collectUseCase.collect(from, to, baseTime);
            if (result.collected() > 0 || "daily".equals(mode)) {
                log.info("analyst report collect completed. mode={} collected={} skipped={}",
                        mode, result.collected(), result.skipped());
            }
        } catch (Exception e) {
            log.error("analyst report collect failed. mode={}", mode, e);
        }
    }

    private void downloadPdfs() {
        try {
            AnalystReportPdfDownloadResult result = downloadUseCase.downloadAll();
            if (result.downloaded() + result.failed() > 0) {
                log.info("analyst report pdf download completed. downloaded={} failed={}",
                        result.downloaded(), result.failed());
            }
        } catch (Exception e) {
            log.error("analyst report pdf download failed.", e);
        }
    }

    private void extractEps(Instant baseTime) {
        try {
            AnalystReportEpsExtractResult result = extractUseCase.extractAll(baseTime);
            if (result.extracted() + result.noSummaryTable() > 0) {
                log.info("analyst report eps extract completed. extracted={} noSummaryTable={} failed={}",
                        result.extracted(), result.noSummaryTable(), result.failed());
            }
        } catch (Exception e) {
            log.error("analyst report eps extract failed.", e);
        }
    }

    private void purgePdfs(Instant baseTime) {
        try {
            AnalystReportPdfPurgeResult result = purgeUseCase.purgeAll(baseTime);
            if (result.purged() + result.failed() > 0) {
                log.info("analyst report pdf purge completed. purged={} failed={}",
                        result.purged(), result.failed());
            }
        } catch (Exception e) {
            log.error("analyst report pdf purge failed.", e);
        }
    }
}
