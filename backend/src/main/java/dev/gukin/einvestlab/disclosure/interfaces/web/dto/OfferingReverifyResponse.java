package dev.gukin.einvestlab.disclosure.interfaces.web.dto;

import dev.gukin.einvestlab.disclosure.application.OfferingReverifyResult;

public record OfferingReverifyResponse(int recovered, int stillFailed) {

    public static OfferingReverifyResponse from(OfferingReverifyResult result) {
        return new OfferingReverifyResponse(result.recovered(), result.stillFailed());
    }
}
