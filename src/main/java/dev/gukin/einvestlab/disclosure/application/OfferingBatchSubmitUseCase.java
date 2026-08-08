package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentSlicer;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import dev.gukin.einvestlab.disclosure.domain.OfferingBatchClient;
import dev.gukin.einvestlab.disclosure.domain.OfferingBatchRepository;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionBatch;
import dev.gukin.einvestlab.global.config.OfferingExtractionProperties;
import dev.gukin.einvestlab.global.id.Ids;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfferingBatchSubmitUseCase {

    private final BusinessContentRepository contentRepository;
    private final OfferingBatchRepository batchRepository;
    private final OfferingResultRecorder recorder;
    private final BusinessContentSlicer slicer;
    private final OfferingBatchClient batchClient;
    private final OfferingExtractionProperties properties;

    public OfferingBatchSubmitResult submit(String model, Instant baseTime) {
        String effectiveModel = model != null ? model : properties.models().getFirst();

        Map<String, String> slices = new LinkedHashMap<>();
        int sliceFailed = 0;
        for (BusinessContent content : contentRepository.findAllPendingOfferingExtraction()) {
            try {
                slices.put(content.getFilingNumber(), slicer.slice(content.getContent()));
            } catch (DisclosureSourceException e) {
                log.warn("슬라이스 실패 (filing={}): {}", content.getFilingNumber(), e.getMessage());
                recorder.recordFailure(content,
                        java.util.List.of("슬라이스 실패: " + e.getMessage()), null);
                sliceFailed++;
            }
        }
        if (slices.isEmpty()) {
            return new OfferingBatchSubmitResult(0, sliceFailed, null);
        }

        String providerBatchId = batchClient.submit(slices, effectiveModel);
        batchRepository.save(OfferingExtractionBatch.builder()
                .id(Ids.generate())
                .providerBatchId(providerBatchId)
                .model(effectiveModel)
                .requestCount(slices.size())
                .status(OfferingExtractionBatch.Status.SUBMITTED)
                .submittedAt(baseTime)
                .build());
        return new OfferingBatchSubmitResult(slices.size(), sliceFailed, providerBatchId);
    }
}
