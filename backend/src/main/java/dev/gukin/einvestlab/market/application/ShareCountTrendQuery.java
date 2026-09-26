package dev.gukin.einvestlab.market.application;

import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.market.domain.ShareCountTrend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareCountTrendQuery {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final int LISTED_CUTOFF_DAYS = 14;

    // 소각 계단은 통상 한 자릿수 %. 이를 크게 넘는 단일 감소는 감자·병합류로 보고 감소 랭킹에서 제외.
    private static final BigDecimal CAPITAL_REDUCTION_DROP_PCT = new BigDecimal("20");

    // 1:1 무상증자가 정확히 +100%, 액면분할은 그 이상 — 지분율 불변의 기계적 증가라 희석 랭킹에서 제외.
    private static final BigDecimal MECHANICAL_RISE_PCT = new BigDecimal("100");

    private final DailyStockPriceRepository repository;

    public List<ShareCountTrend> rank(int years, boolean decreasing, int limit, Instant baseTime) {
        LocalDate baseDate = LocalDate.ofInstant(baseTime, KOREA);
        return repository.findShareCountTrends(
                baseDate.minusYears(years),
                baseDate.minusDays(LISTED_CUTOFF_DAYS),
                decreasing,
                decreasing ? CAPITAL_REDUCTION_DROP_PCT : null,
                decreasing ? null : MECHANICAL_RISE_PCT,
                limit);
    }
}
