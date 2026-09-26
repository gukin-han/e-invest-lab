package dev.gukin.einvestlab.disclosure.interfaces.web.dto;

import dev.gukin.einvestlab.disclosure.application.OfferingBatchSubmitResult;

public record OfferingBatchSubmitResponse(int submitted, int sliceFailed, String providerBatchId) {

    public static OfferingBatchSubmitResponse from(OfferingBatchSubmitResult result) {
        return new OfferingBatchSubmitResponse(
                result.submitted(), result.sliceFailed(), result.providerBatchId());
    }
}
