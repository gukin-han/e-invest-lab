package dev.gukin.einvestlab.disclosure.application;

public record OfferingBatchSubmitResult(int submitted, int sliceFailed, String providerBatchId) {
}
