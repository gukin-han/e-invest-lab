package dev.gukin.einvestlab.company.infra.db;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.company.domain.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepository {

    private final CompanyJpaRepository jpa;
    private final CompanyJdbcRepository jdbc;

    @Override
    public Company save(Company company) {
        return jpa.save(company);
    }

    @Override
    public int upsertCompanies(List<Company> companies) {
        return jdbc.upsertCompanies(companies);
    }

    @Override
    public Optional<Company> findByCorpCode(String corpCode) {
        return jpa.findByCorpCode(corpCode);
    }
}
