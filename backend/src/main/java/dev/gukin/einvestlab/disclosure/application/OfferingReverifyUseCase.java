package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfferingReverifyUseCase {

    private final BusinessContentRepository contentRepository;
    private final OfferingGuard guard;
    private final OfferingResultRecorder recorder;

    public OfferingReverifyResult reverifyAll(Instant baseTime) {
        int recovered = 0;
        int stillFailed = 0;
        for (BusinessContent content : contentRepository.findAllFailedWithDrafts()) {
            OfferingGuardVerdict verdict = guard.verify(
                    recorder.readDrafts(content.getOfferingExtractionDrafts()), content.getContent());
            if (verdict.passed()) {
                recorder.record(content, verdict, baseTime);
                recovered++;
            } else {
                recorder.recordFailure(content, verdict.issues(),
                        recorder.readDrafts(content.getOfferingExtractionDrafts()));
                stillFailed++;
            }
        }
        return new OfferingReverifyResult(recovered, stillFailed);
    }
}
