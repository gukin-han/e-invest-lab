package dev.gukin.einvestlab.disclosure.infra.db;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
