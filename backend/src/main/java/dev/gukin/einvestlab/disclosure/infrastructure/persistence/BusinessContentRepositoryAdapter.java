package dev.gukin.einvestlab.disclosure.infrastructure.persistence;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BusinessContentRepositoryAdapter implements BusinessContentRepository {

    private final BusinessContentJpaRepository jpa;

    @Override
    public BusinessContent save(BusinessContent businessContent) {
        return jpa.save(businessContent);
    }

    @Override
    public boolean existsByFilingNumber(String filingNumber) {
        return jpa.existsByFilingNumber(filingNumber);
    }

    @Override
    public List<BusinessContent> findAllPendingOfferingExtraction() {
        return jpa.findAllPendingOfferingExtraction(OfferingExtractionStatus.FAILED);
    }

    @Override
    public List<BusinessContent> findAllFailedWithDrafts() {
        return jpa.findAllByOfferingExtractionStatusAndOfferingExtractionDraftsIsNotNull(
                OfferingExtractionStatus.FAILED);
    }

    @Override
    public Optional<BusinessContent> findByFilingNumber(String filingNumber) {
        return jpa.findByFilingNumber(filingNumber);
    }
}
