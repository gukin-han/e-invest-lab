package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportListing;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import dev.gukin.einvestlab.research.domain.AnalystReportSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AnalystReportCollectUseCase {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LOOKBACK_DAYS = 7;

    private final AnalystReportSource source;
    private final AnalystReportRepository repository;

    public AnalystReportCollectResult collect(LocalDate start, LocalDate end, Instant baseTime) {
        CollectWindow window = resolveWindow(start, end, baseTime);

        int collected = 0;
        int skipped = 0;
        for (AnalystReportListing listing : source.fetchListings(window.start(), window.end())) {
            if (repository.existsByReportIdx(listing.reportIdx())) {
                skipped++;
                continue;
            }
            repository.save(toReport(listing, baseTime));
            collected++;
        }
        return new AnalystReportCollectResult(collected, skipped);
    }

    private CollectWindow resolveWindow(LocalDate start, LocalDate end, Instant baseTime) {
        LocalDate effectiveEnd = end != null ? end : LocalDate.ofInstant(baseTime, KOREA);
        LocalDate effectiveStart = start != null ? start : effectiveEnd.minusDays(DEFAULT_LOOKBACK_DAYS);
        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new IllegalArgumentException(
                    "수집 기간 역전: " + effectiveStart + " > " + effectiveEnd);
        }
        return new CollectWindow(effectiveStart, effectiveEnd);
    }

    private record CollectWindow(LocalDate start, LocalDate end) {
    }

    private AnalystReport toReport(AnalystReportListing listing, Instant baseTime) {
        return AnalystReport.builder()
                .id(Ids.generate())
                .reportIdx(listing.reportIdx())
                .stockCode(listing.stockCode())
                .companyName(listing.companyName())
                .title(listing.title())
                .broker(listing.broker())
                .authors(listing.authors())
                .publishedDate(listing.publishedDate())
                .targetPrice(listing.targetPrice())
                .opinion(listing.opinion())
                .collectedAt(baseTime)
                .build();
    }
}
