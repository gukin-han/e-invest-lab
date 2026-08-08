package dev.gukin.einvestlab.disclosure.application;

public record OfferingBatchCollectResult(
        int collectedBatches, int stillRunning, int extracted, int corrected, int failed) {
}
