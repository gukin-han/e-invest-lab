package dev.gukin.einvestlab.disclosure.infrastructure.persistence;

import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfferingBatchJpaRepository extends JpaRepository<OfferingExtractionBatch, UUID> {

    List<OfferingExtractionBatch> findAllByStatus(OfferingExtractionBatch.Status status);
}
