package dev.gukin.einvestlab.company.interfaces.web;

import dev.gukin.einvestlab.company.application.CompanyRegistrySyncResult;
import dev.gukin.einvestlab.company.application.CompanyRegistrySyncUseCase;
import dev.gukin.einvestlab.global.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CompanyRegistryController {

    private final CompanyRegistrySyncUseCase syncUseCase;

    @PostMapping("/internal/company-registry/sync")
    public ApiResponse<CompanyRegistrySyncResponse> sync() {
        CompanyRegistrySyncResult result = syncUseCase.syncAll();
        return ApiResponse.of(CompanyRegistrySyncResponse.from(result));
    }
}
