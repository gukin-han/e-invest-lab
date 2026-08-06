package dev.gukin.einvestlab.research.domain;

import java.util.List;

public interface EpsEstimateRepository {

    void saveAll(List<EpsEstimate> estimates);
}
