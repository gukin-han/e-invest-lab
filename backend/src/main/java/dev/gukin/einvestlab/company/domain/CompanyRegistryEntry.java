package dev.gukin.einvestlab.company.domain;

import java.time.LocalDate;

public record CompanyRegistryEntry(
        String corpCode,
        String name,
        String englishName,
        String stockCode,
        LocalDate registryModifiedDate
) {
}
