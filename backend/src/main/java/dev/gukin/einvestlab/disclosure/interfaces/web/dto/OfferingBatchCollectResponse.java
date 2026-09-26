package dev.gukin.einvestlab.disclosure.interfaces.web.dto;

import dev.gukin.einvestlab.disclosure.application.OfferingBatchCollectResult;

public record OfferingBatchCollectResponse(
        int collectedBatches, int stillRunning, int extracted, int corrected, int failed) {

    public static OfferingBatchCollectResponse from(OfferingBatchCollectResult result) {
        return new OfferingBatchCollectResponse(
                result.collectedBatches(), result.stillRunning(),
                result.extracted(), result.corrected(), result.failed());
    }
}
