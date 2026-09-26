package dev.gukin.einvestlab.disclosure.interfaces.web;

import dev.gukin.einvestlab.disclosure.application.OfferingExtractResult;
import dev.gukin.einvestlab.disclosure.application.OfferingExtractUseCase;
import dev.gukin.einvestlab.disclosure.interfaces.web.dto.OfferingExtractResponse;
import dev.gukin.einvestlab.global.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequiredArgsConstructor
public class OfferingExtractController {

    private final OfferingExtractUseCase extractUseCase;
    private final Clock clock;

    @PostMapping("/internal/offerings/extract")
    public ApiResponse<OfferingExtractResponse> extract() {
        OfferingExtractResult result = extractUseCase.extractAll(clock.instant());
        return ApiResponse.of(OfferingExtractResponse.from(result));
    }
}
