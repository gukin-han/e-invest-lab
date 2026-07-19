package dev.gukin.einvestlab.disclosure.interfaces.web;

import dev.gukin.einvestlab.disclosure.application.BusinessContentCollectResult;
import dev.gukin.einvestlab.disclosure.application.BusinessContentCollectUseCase;
import dev.gukin.einvestlab.global.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequiredArgsConstructor
public class BusinessContentCollectController {

    private final BusinessContentCollectUseCase collectUseCase;
    private final Clock clock;

    @PostMapping("/internal/business-contents/{corpCode}/collect")
    public ApiResponse<BusinessContentCollectResponse> collect(@PathVariable String corpCode) {
        BusinessContentCollectResult result = collectUseCase.collect(corpCode, clock.instant());
        return ApiResponse.of(BusinessContentCollectResponse.from(result));
    }
}
