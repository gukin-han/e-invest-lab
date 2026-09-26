package dev.gukin.einvestlab.disclosure.interfaces.web.dto;

import dev.gukin.einvestlab.disclosure.application.OfferingExtractResult;

public record OfferingExtractResponse(int extracted, int corrected, int failed, int escalated) {

    public static OfferingExtractResponse from(OfferingExtractResult result) {
        return new OfferingExtractResponse(
                result.extracted(), result.corrected(), result.failed(), result.escalated());
    }
}
