package dev.gukin.einvestlab.company.domain;

import java.util.function.Consumer;

public interface CompanyRegistrySource {

    void streamAll(Consumer<Company> handler);
}
