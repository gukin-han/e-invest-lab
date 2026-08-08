package dev.gukin.einvestlab.disclosure.infrastructure.persistence;

import dev.gukin.einvestlab.disclosure.domain.OfferingBatchRepository;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionBatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OfferingBatchRepositoryAdapter implements OfferingBatchRepository {

    private final OfferingBatchJpaRepository jpa;

    @Override
    public OfferingExtractionBatch save(OfferingExtractionBatch batch) {
        return jpa.save(batch);
    }

    @Override
    public List<OfferingExtractionBatch> findAllSubmitted() {
        return jpa.findAllByStatus(OfferingExtractionBatch.Status.SUBMITTED);
    }
}
