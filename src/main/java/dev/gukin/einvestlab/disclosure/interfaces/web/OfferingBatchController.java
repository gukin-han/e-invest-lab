package dev.gukin.einvestlab.disclosure.interfaces.web;

import dev.gukin.einvestlab.disclosure.application.OfferingBatchCollectResult;
import dev.gukin.einvestlab.disclosure.application.OfferingBatchCollectUseCase;
import dev.gukin.einvestlab.disclosure.application.OfferingBatchSubmitResult;
import dev.gukin.einvestlab.disclosure.application.OfferingBatchSubmitUseCase;
import dev.gukin.einvestlab.disclosure.application.OfferingReverifyResult;
import dev.gukin.einvestlab.disclosure.application.OfferingReverifyUseCase;
import dev.gukin.einvestlab.disclosure.interfaces.web.dto.OfferingBatchCollectResponse;
import dev.gukin.einvestlab.disclosure.interfaces.web.dto.OfferingBatchSubmitResponse;
import dev.gukin.einvestlab.disclosure.interfaces.web.dto.OfferingReverifyResponse;
import dev.gukin.einvestlab.global.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequiredArgsConstructor
public class OfferingBatchController {

    private final OfferingBatchSubmitUseCase submitUseCase;
    private final OfferingBatchCollectUseCase collectUseCase;
    private final OfferingReverifyUseCase reverifyUseCase;
    private final Clock clock;

    @PostMapping("/internal/offerings/batch-submit")
    public ApiResponse<OfferingBatchSubmitResponse> submit(
            @RequestParam(required = false) String model) {
        OfferingBatchSubmitResult result = submitUseCase.submit(model, clock.instant());
        return ApiResponse.of(OfferingBatchSubmitResponse.from(result));
    }

    @PostMapping("/internal/offerings/batch-collect")
    public ApiResponse<OfferingBatchCollectResponse> collect() {
        OfferingBatchCollectResult result = collectUseCase.collect(clock.instant());
        return ApiResponse.of(OfferingBatchCollectResponse.from(result));
    }

    @PostMapping("/internal/offerings/reverify")
    public ApiResponse<OfferingReverifyResponse> reverify() {
        OfferingReverifyResult result = reverifyUseCase.reverifyAll(clock.instant());
        return ApiResponse.of(OfferingReverifyResponse.from(result));
    }
}
