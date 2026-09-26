package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.OfferingBatchClient;
import dev.gukin.einvestlab.disclosure.domain.OfferingBatchClient.BatchOutcome;
import dev.gukin.einvestlab.disclosure.domain.OfferingBatchRepository;
import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfferingBatchCollectUseCase {

    private final OfferingBatchRepository batchRepository;
    private final BusinessContentRepository contentRepository;
    private final OfferingBatchClient batchClient;
    private final OfferingGuard guard;
    private final OfferingResultRecorder recorder;

    public OfferingBatchCollectResult collect(Instant baseTime) {
        int stillRunning = 0;
        int collectedBatches = 0;
        int extracted = 0;
        int corrected = 0;
        int failed = 0;
        for (OfferingExtractionBatch batch : batchRepository.findAllSubmitted()) {
            BatchOutcome outcome = batchClient.fetchOutcome(batch.getProviderBatchId());
            switch (outcome.state()) {
                case IN_PROGRESS -> stillRunning++;
                case FAILED -> {
                    batch.markFailed(baseTime);
                    batchRepository.save(batch);
                }
                case COMPLETED -> {
                    Tally tally = apply(outcome, baseTime);
                    extracted += tally.extracted();
                    corrected += tally.corrected();
                    failed += tally.failed();
                    batch.markCollected(baseTime);
                    batchRepository.save(batch);
                    collectedBatches++;
                }
            }
        }
        return new OfferingBatchCollectResult(collectedBatches, stillRunning, extracted, corrected, failed);
    }

    private Tally apply(BatchOutcome outcome, Instant baseTime) {
        int extracted = 0;
        int corrected = 0;
        int failed = 0;
        for (Map.Entry<String, List<OfferingDraft>> entry : outcome.draftsByFilingNumber().entrySet()) {
            Optional<BusinessContent> found = contentRepository.findByFilingNumber(entry.getKey());
            if (found.isEmpty()) {
                log.warn("배치 결과의 filing 을 찾을 수 없음: {}", entry.getKey());
                continue;
            }
            BusinessContent content = found.get();
            OfferingGuardVerdict verdict = guard.verify(entry.getValue(), content.getContent());
            if (verdict.passed()) {
                recorder.record(content, verdict, baseTime);
                switch (verdict.status()) {
                    case EXTRACTED -> extracted++;
                    case CORRECTED -> corrected++;
                    default -> {
                    }
                }
            } else {
                log.warn("가드 실패 (filing={}): {}", entry.getKey(), verdict.issues());
                recorder.recordFailure(content, verdict.issues(), entry.getValue());
                failed++;
            }
        }
        for (String filingNumber : outcome.failedFilingNumbers()) {
            contentRepository.findByFilingNumber(filingNumber).ifPresent(content ->
                    recorder.recordFailure(content, List.of("배치 응답 실패"), null));
            failed++;
        }
        return new Tally(extracted, corrected, failed);
    }

    private record Tally(int extracted, int corrected, int failed) {
    }
}
