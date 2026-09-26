package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import dev.gukin.einvestlab.research.domain.CoveredStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockCoverageQuery {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 90;

    private final AnalystReportRepository reportRepository;

    public List<CoveredStock> recentlyCovered(Integer days, Instant baseTime) {
        int effectiveDays = days != null ? days : DEFAULT_DAYS;
        if (effectiveDays < 1 || effectiveDays > MAX_DAYS) {
            throw new IllegalArgumentException("조회 기간은 1~" + MAX_DAYS + "일: " + effectiveDays);
        }
        LocalDate since = LocalDate.ofInstant(baseTime, KOREA).minusDays(effectiveDays);
        return reportRepository.findRecentlyCovered(since);
    }
}
