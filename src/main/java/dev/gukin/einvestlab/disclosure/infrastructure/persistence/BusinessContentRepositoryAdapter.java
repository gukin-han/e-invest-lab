package dev.gukin.einvestlab.disclosure.infrastructure.persistence;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
