package dev.gukin.einvestlab.disclosure.domain;

import java.util.List;

public interface BusinessContentRepository {

    BusinessContent save(BusinessContent businessContent);

    boolean existsByFilingNumber(String filingNumber);

    List<BusinessContent> findAllPendingOfferingExtraction();
}
