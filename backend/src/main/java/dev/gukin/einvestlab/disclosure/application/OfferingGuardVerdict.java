package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;

import java.util.List;

public record OfferingGuardVerdict(
        OfferingExtractionStatus status,
        List<OfferingDraft> accepted,
        List<String> issues
) {

    public static OfferingGuardVerdict failed(List<String> issues) {
        return new OfferingGuardVerdict(OfferingExtractionStatus.FAILED, List.of(), issues);
    }

    public boolean passed() {
        return status != OfferingExtractionStatus.FAILED;
    }
}
