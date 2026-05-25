package dev.gukin.einvestlab.company.domain;

import java.util.Optional;

public interface CompanyRepository {

    Company save(Company company);

    Optional<Company> findByCorpCode(String corpCode);
}
