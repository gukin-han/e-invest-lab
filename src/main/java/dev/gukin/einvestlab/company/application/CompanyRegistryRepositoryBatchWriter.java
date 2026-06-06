package dev.gukin.einvestlab.company.application;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.company.domain.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CompanyRegistryRepositoryBatchWriter implements CompanyRegistryBatchWriter {

    private final CompanyRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int upsert(List<Company> companies) {
        return repository.upsertCompanies(companies);
    }
}
