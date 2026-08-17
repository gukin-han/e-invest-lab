package dev.gukin.einvestlab.research.domain;

import java.time.LocalDate;
import java.util.List;

public interface EpsEstimateRepository {

    void saveAll(List<EpsEstimate> estimates);

    void deleteAllByReportIdx(long reportIdx);

    List<EpsEstimate> findAllByReportIdx(long reportIdx);

    List<EpsConsensus> findConsensus(String stockCode, LocalDate since, String excludedBroker);

    List<EpsRevision> findRevisions(String stockCode, int fiscalYear);
}
