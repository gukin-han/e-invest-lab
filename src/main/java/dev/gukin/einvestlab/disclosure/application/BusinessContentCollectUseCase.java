package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.BusinessReportFiling;
import dev.gukin.einvestlab.disclosure.domain.BusinessReportSource;
import dev.gukin.einvestlab.global.id.Ids;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BusinessContentCollectUseCase {

    private final BusinessReportSource businessReportSource;
    private final BusinessContentRepository businessContentRepository;

    public BusinessContentCollectResult collect(String corpCode, Instant baseTime) {
        Optional<BusinessReportFiling> latest = businessReportSource.findLatest(corpCode, baseTime);
        if (latest.isEmpty()) {
            return BusinessContentCollectResult.NO_REPORT;
        }
        BusinessReportFiling filing = latest.get();
        if (businessContentRepository.existsByFilingNumber(filing.filingNumber())) {
            return BusinessContentCollectResult.ALREADY_COLLECTED;
        }
        String content = businessReportSource.fetchBusinessContent(filing);
        businessContentRepository.save(BusinessContent.builder()
                .id(Ids.generate())
                .corpCode(filing.corpCode())
                .filingNumber(filing.filingNumber())
                .filedDate(filing.filedDate())
                .content(content)
                .collectedAt(baseTime)
                .build());
        return BusinessContentCollectResult.COLLECTED;
    }
}
