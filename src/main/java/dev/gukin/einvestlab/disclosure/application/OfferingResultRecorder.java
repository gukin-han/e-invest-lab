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

import java.time.Instant;

@Component
public class OfferingResultRecorder {

    private final BusinessContentRepository contentRepository;
    private final OfferingRepository offeringRepository;
    private final TransactionTemplate contentTransaction;

    public OfferingResultRecorder(BusinessContentRepository contentRepository,
                                  OfferingRepository offeringRepository,
                                  PlatformTransactionManager transactionManager) {
        this.contentRepository = contentRepository;
        this.offeringRepository = offeringRepository;
        this.contentTransaction = new TransactionTemplate(transactionManager);
    }

    public void record(BusinessContent content, OfferingGuardVerdict verdict, Instant baseTime) {
        contentTransaction.executeWithoutResult(tx -> {
            offeringRepository.deleteAllByFilingNumber(content.getFilingNumber());
            offeringRepository.saveAll(verdict.accepted().stream()
                    .map(draft -> toOffering(content, draft, baseTime))
                    .toList());
            content.recordOfferingExtraction(verdict.status());
            contentRepository.save(content);
        });
    }

    public void recordFailure(BusinessContent content) {
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
}
