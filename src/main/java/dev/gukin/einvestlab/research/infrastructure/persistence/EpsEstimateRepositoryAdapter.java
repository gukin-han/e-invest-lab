package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.EpsConsensus;
import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.domain.EpsRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EpsEstimateRepositoryAdapter implements EpsEstimateRepository {

    private final EpsEstimateJpaRepository jpa;

    @Override
    public void saveAll(List<EpsEstimate> estimates) {
        jpa.saveAll(estimates);
    }

    @Override
    public void deleteAllByReportIdx(long reportIdx) {
        jpa.deleteAllByReportIdx(reportIdx);
    }

    @Override
    public List<EpsEstimate> findAllByReportIdx(long reportIdx) {
        return jpa.findAllByReportIdxOrderByFiscalYear(reportIdx);
    }

    @Override
    public List<EpsConsensus> findConsensus(String stockCode, LocalDate since, String excludedBroker) {
        return jpa.findConsensus(stockCode, since, excludedBroker).stream()
                .map(row -> new EpsConsensus(
                        row.getFiscalYear(),
                        row.getAverageEps(),
                        row.getSampleCount(),
                        row.getMinEps(),
                        row.getMaxEps()))
                .toList();
    }

    @Override
    public List<EpsRevision> findRevisions(String stockCode, int fiscalYear) {
        return jpa.findRevisions(stockCode, fiscalYear).stream()
                .map(row -> new EpsRevision(
                        row.getPublishedDate(),
                        row.getBroker(),
                        row.getEps(),
                        row.getEstimated()))
                .toList();
    }
}
