package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentSlicer;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import dev.gukin.einvestlab.disclosure.domain.Offering;
import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionException;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractor;
import dev.gukin.einvestlab.disclosure.domain.OfferingRepository;
import dev.gukin.einvestlab.global.config.OfferingExtractionProperties;
import dev.gukin.einvestlab.global.id.Ids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class OfferingExtractUseCase {

    private final BusinessContentRepository contentRepository;
    private final OfferingRepository offeringRepository;
    private final BusinessContentSlicer slicer;
    private final OfferingExtractor extractor;
    private final OfferingGuard guard;
    private final OfferingExtractionProperties properties;
    private final TransactionTemplate contentTransaction;

    public OfferingExtractUseCase(BusinessContentRepository contentRepository,
                                  OfferingRepository offeringRepository,
                                  BusinessContentSlicer slicer,
                                  OfferingExtractor extractor,
                                  OfferingGuard guard,
                                  OfferingExtractionProperties properties,
                                  PlatformTransactionManager transactionManager) {
        this.contentRepository = contentRepository;
        this.offeringRepository = offeringRepository;
        this.slicer = slicer;
        this.extractor = extractor;
        this.guard = guard;
        this.properties = properties;
        this.contentTransaction = new TransactionTemplate(transactionManager);
    }

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
            markFailed(content);
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
                save(content, verdict, baseTime);
                if (!verdict.issues().isEmpty()) {
                    log.info("가드 교정 (filing={}, model={}): {}",
                            content.getFilingNumber(), model, verdict.issues());
                }
                return new Outcome(verdict.status(), escalatedAttempt);
            }
            log.warn("가드 실패 (filing={}, model={}): {}",
                    content.getFilingNumber(), model, verdict.issues());
        }
        markFailed(content);
        return new Outcome(OfferingExtractionStatus.FAILED, models.size() > 1);
    }

    private void save(BusinessContent content, OfferingGuardVerdict verdict, Instant baseTime) {
        contentTransaction.executeWithoutResult(tx -> {
            offeringRepository.deleteAllByFilingNumber(content.getFilingNumber());
            offeringRepository.saveAll(verdict.accepted().stream()
                    .map(draft -> toOffering(content, draft, baseTime))
                    .toList());
            content.recordOfferingExtraction(verdict.status());
            contentRepository.save(content);
        });
    }

    private void markFailed(BusinessContent content) {
        contentTransaction.executeWithoutResult(tx -> {
            content.recordOfferingExtraction(OfferingExtractionStatus.FAILED);
            contentRepository.save(content);
        });
    }

    private Offering toOffering(BusinessContent content, OfferingDraft draft, Instant baseTime) {
        return Offering.builder()
                .id(Ids.generate())
                .corpCode(content.getCorpCode())
                .filingNumber(content.getFilingNumber())
                .businessPart(draft.businessPart())
                .segment(draft.segment())
                .qualifier(draft.qualifier())
                .products(draft.products())
                .revenueAmount(draft.revenueAmount())
                .revenueUnit(draft.revenueUnit())
                .revenueBasis(draft.revenueBasis())
                .revenueShare(draft.revenueShare())
                .customers(draft.customers())
                .entityName(draft.entityName())
                .fiscalYear(draft.fiscalYear())
                .extractedAt(baseTime)
                .build();
    }

    private record Outcome(OfferingExtractionStatus status, boolean escalated) {
    }
}
