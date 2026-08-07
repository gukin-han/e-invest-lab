package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.research.domain.EpsConsensus;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.domain.EpsRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EpsStatisticsQuery {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final int VALID_MONTHS = 6;

    private final EpsEstimateRepository estimateRepository;

    public List<EpsConsensus> consensus(String stockCode, Instant baseTime) {
        LocalDate since = LocalDate.ofInstant(baseTime, KOREA).minusMonths(VALID_MONTHS);
        return estimateRepository.findConsensus(stockCode, since);
    }

    public List<EpsRevision> revisions(String stockCode, int fiscalYear) {
        return estimateRepository.findRevisions(stockCode, fiscalYear);
    }
}
