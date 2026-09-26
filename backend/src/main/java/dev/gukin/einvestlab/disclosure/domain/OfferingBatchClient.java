package dev.gukin.einvestlab.disclosure.domain;

import java.util.List;
import java.util.Map;

public interface OfferingBatchClient {

    String submit(Map<String, String> slicesByFilingNumber, String model);

    BatchOutcome fetchOutcome(String providerBatchId);

    record BatchOutcome(State state, Map<String, List<OfferingDraft>> draftsByFilingNumber,
                        List<String> failedFilingNumbers) {

        public enum State {IN_PROGRESS, COMPLETED, FAILED}

        public static BatchOutcome inProgress() {
            return new BatchOutcome(State.IN_PROGRESS, Map.of(), List.of());
        }

        public static BatchOutcome failed() {
            return new BatchOutcome(State.FAILED, Map.of(), List.of());
        }
    }
}
