package dev.gukin.einvestlab.disclosure.domain;

import java.util.List;
import java.util.Optional;

public interface BusinessContentRepository {

    BusinessContent save(BusinessContent businessContent);

    boolean existsByFilingNumber(String filingNumber);

    List<BusinessContent> findAllPendingOfferingExtraction();

    List<BusinessContent> findAllFailedWithDrafts();

    Optional<BusinessContent> findByFilingNumber(String filingNumber);
}
