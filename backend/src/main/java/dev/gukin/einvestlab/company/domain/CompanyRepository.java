package dev.gukin.einvestlab.company.domain;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository {

    Company save(Company company);

    int upsertCompanies(List<Company> companies);

    Optional<Company> findByCorpCode(String corpCode);
}
