package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentSlicer;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionException;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractor;
import dev.gukin.einvestlab.global.config.OfferingExtractionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfferingExtractUseCase {

    private final BusinessContentRepository contentRepository;
    private final OfferingResultRecorder recorder;
    private final BusinessContentSlicer slicer;
    private final OfferingExtractor extractor;
    private final OfferingGuard guard;
    private final OfferingExtractionProperties properties;

    public OfferingExtractResult extractAll(Instant baseTime) {
        int extracted = 0;
        int corrected = 0;
        int failed = 0;
        int escalated = 0;
        for (BusinessContent content : contentRepository.findAllPendingOfferingExtraction()) {
            Outcome outcome = extractOne(content, baseTime);
            switch (outcome.status()) {
                case EXTRACTED -> extracted++;
                case CORRECTED -> corrected++;
                case FAILED -> failed++;
            }
            if (outcome.escalated()) {
                escalated++;
            }
        }
        return new OfferingExtractResult(extracted, corrected, failed, escalated);
    }

    private Outcome extractOne(BusinessContent content, Instant baseTime) {
        String slice;
        try {
            slice = slicer.slice(content.getContent());
        } catch (DisclosureSourceException e) {
            log.warn("슬라이스 실패 (filing={}): {}", content.getFilingNumber(), e.getMessage());
            recorder.recordFailure(content);
            return new Outcome(OfferingExtractionStatus.FAILED, false);
        }

        List<String> models = properties.models();
        for (int i = 0; i < models.size(); i++) {
            String model = models.get(i);
            boolean escalatedAttempt = i > 0;
            List<OfferingDraft> drafts;
            try {
                drafts = extractor.extract(slice, model);
            } catch (OfferingExtractionException e) {
                log.warn("추출 호출 실패 (filing={}, model={}): {}",
                        content.getFilingNumber(), model, e.getMessage());
                continue;
            }
            OfferingGuardVerdict verdict = guard.verify(drafts, content.getContent());
            if (verdict.passed()) {
                recorder.record(content, verdict, baseTime);
                if (!verdict.issues().isEmpty()) {
                    log.info("가드 교정 (filing={}, model={}): {}",
                            content.getFilingNumber(), model, verdict.issues());
                }
                return new Outcome(verdict.status(), escalatedAttempt);
            }
            log.warn("가드 실패 (filing={}, model={}): {}",
                    content.getFilingNumber(), model, verdict.issues());
        }
        recorder.recordFailure(content);
        return new Outcome(OfferingExtractionStatus.FAILED, models.size() > 1);
    }

    private record Outcome(OfferingExtractionStatus status, boolean escalated) {
    }
}
