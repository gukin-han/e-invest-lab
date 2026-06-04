package dev.gukin.einvestlab.company.infra.http;

import dev.gukin.einvestlab.company.domain.Company;

import java.util.function.Consumer;

public interface CompanyMasterClient {

    void streamAll(Consumer<Company> handler);
}
