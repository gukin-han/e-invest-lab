package dev.gukin.einvestlab.research.domain;

import java.time.LocalDate;
import java.util.List;

public interface EpsEstimateRepository {

    void saveAll(List<EpsEstimate> estimates);

    List<EpsConsensus> findConsensus(String stockCode, LocalDate since);

    List<EpsRevision> findRevisions(String stockCode, int fiscalYear);
}
