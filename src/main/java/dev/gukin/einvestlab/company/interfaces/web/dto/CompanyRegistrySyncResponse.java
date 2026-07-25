package dev.gukin.einvestlab.company.interfaces.web.dto;

import dev.gukin.einvestlab.company.application.CompanyRegistrySyncResult;

public record CompanyRegistrySyncResponse(int upsertedCount) {

    static CompanyRegistrySyncResponse from(CompanyRegistrySyncResult result) {
        return new CompanyRegistrySyncResponse(result.upsertedCount());
    }
}
