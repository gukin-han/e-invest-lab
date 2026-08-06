package dev.gukin.einvestlab.research.infrastructure.persistence;

import dev.gukin.einvestlab.research.domain.EpsEstimate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EpsEstimateJpaRepository extends JpaRepository<EpsEstimate, UUID> {
}
