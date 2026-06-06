package dev.gukin.einvestlab.company.application;

import dev.gukin.einvestlab.company.domain.Company;

import java.util.List;

public interface CompanyRegistryBatchWriter {

    int upsert(List<Company> companies);
}
