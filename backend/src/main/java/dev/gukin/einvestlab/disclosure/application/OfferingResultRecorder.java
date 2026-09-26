package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.Offering;
import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;
import dev.gukin.einvestlab.disclosure.domain.OfferingRepository;
import dev.gukin.einvestlab.global.id.Ids;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Component
public class OfferingResultRecorder {

    private static final int NOTE_LIMIT = 2_000;

    private final BusinessContentRepository contentRepository;
    private final OfferingRepository offeringRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate contentTransaction;

    public OfferingResultRecorder(BusinessContentRepository contentRepository,
                                  OfferingRepository offeringRepository,
                                  ObjectMapper objectMapper,
                                  PlatformTransactionManager transactionManager) {
        this.contentRepository = contentRepository;
        this.offeringRepository = offeringRepository;
        this.objectMapper = objectMapper;
        this.contentTransaction = new TransactionTemplate(transactionManager);
    }

    public void record(BusinessContent content, OfferingGuardVerdict verdict, Instant baseTime) {
        contentTransaction.executeWithoutResult(tx -> {
            offeringRepository.deleteAllByFilingNumber(content.getFilingNumber());
            offeringRepository.saveAll(verdict.accepted().stream()
                    .map(draft -> toOffering(content, draft, baseTime))
                    .toList());
            content.recordOfferingExtraction(verdict.status(), note(verdict.issues()), null);
            contentRepository.save(content);
        });
    }

    public void recordFailure(BusinessContent content, List<String> issues, List<OfferingDraft> drafts) {
        contentTransaction.executeWithoutResult(tx -> {
            content.recordOfferingExtraction(
                    OfferingExtractionStatus.FAILED, note(issues), draftsJson(drafts));
            contentRepository.save(content);
        });
    }

    public List<OfferingDraft> readDrafts(String draftsJson) {
        return objectMapper.readValue(draftsJson, StoredDrafts.class).drafts();
    }

    private String draftsJson(List<OfferingDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(new StoredDrafts(drafts));
    }

    private String note(List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return null;
        }
        String joined = String.join(" | ", issues);
        return joined.length() <= NOTE_LIMIT ? joined : joined.substring(0, NOTE_LIMIT);
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

    record StoredDrafts(List<OfferingDraft> drafts) {
    }
}
