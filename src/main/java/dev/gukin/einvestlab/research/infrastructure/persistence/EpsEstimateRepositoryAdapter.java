package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EpsEstimateRepositoryAdapter implements EpsEstimateRepository {

    private final EpsEstimateJpaRepository jpa;

    @Override
    public void saveAll(List<EpsEstimate> estimates) {
        jpa.saveAll(estimates);
    }
}
