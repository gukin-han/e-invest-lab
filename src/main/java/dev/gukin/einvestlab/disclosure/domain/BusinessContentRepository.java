package dev.gukin.einvestlab.disclosure.domain;

public interface BusinessContentRepository {

    BusinessContent save(BusinessContent businessContent);

    boolean existsByFilingNumber(String filingNumber);
}
