package dev.gukin.einvestlab.market.application;

import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.market.domain.ShareCountTrend;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ShareCountTrendQuery {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final int LISTED_CUTOFF_DAYS = 14;
    private static final int MAX_LIMIT = 200;

    // 소각 계단은 통상 한 자릿수 %. 이를 크게 넘는 단일 감소는 감자·병합류로 보고 감소 랭킹에서 제외.
    private static final BigDecimal CAPITAL_REDUCTION_DROP_PCT = new BigDecimal("20");

    // 원천이 하루 한 번 갱신되는데 LAG 전량 스캔이 십수 초라 결과를 캐시.
    // 근본 해소는 계단 파생 테이블(product-direction.md Phase 1) 몫.
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final DailyStockPriceRepository repository;
    private final Map<String, CachedRanking> cache = new ConcurrentHashMap<>();

    public List<ShareCountTrend> rank(int years, boolean decreasing, int limit, Instant baseTime) {
        String key = years + ":" + decreasing;
        CachedRanking cached = cache.compute(key, (k, existing) ->
                existing != null && existing.freshAt(baseTime)
                        ? existing
                        : new CachedRanking(load(years, decreasing, baseTime), baseTime.plus(CACHE_TTL)));
        return cached.trends().subList(0, Math.min(limit, cached.trends().size()));
    }

    private List<ShareCountTrend> load(int years, boolean decreasing, Instant baseTime) {
        LocalDate baseDate = LocalDate.ofInstant(baseTime, KOREA);
        return repository.findShareCountTrends(
                baseDate.minusYears(years),
                baseDate.minusDays(LISTED_CUTOFF_DAYS),
                decreasing,
                decreasing ? CAPITAL_REDUCTION_DROP_PCT : null,
                MAX_LIMIT);
    }

    private record CachedRanking(List<ShareCountTrend> trends, Instant expiresAt) {
        boolean freshAt(Instant now) {
            return now.isBefore(expiresAt);
        }
    }
}
