package dev.gukin.einvestlab.disclosure.domain;

import java.util.List;

public interface OfferingBatchRepository {

    OfferingExtractionBatch save(OfferingExtractionBatch batch);

    List<OfferingExtractionBatch> findAllSubmitted();
}
