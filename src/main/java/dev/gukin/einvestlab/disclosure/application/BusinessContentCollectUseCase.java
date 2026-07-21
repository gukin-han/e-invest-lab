package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.BusinessContent;
import dev.gukin.einvestlab.disclosure.domain.BusinessContentRepository;
import dev.gukin.einvestlab.disclosure.domain.BusinessReportFiling;
import dev.gukin.einvestlab.disclosure.domain.BusinessReportSource;
import dev.gukin.einvestlab.disclosure.domain.DisclosureDocumentMissingException;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import dev.gukin.einvestlab.global.id.Ids;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessContentCollectUseCase {

    private final BusinessReportSource businessReportSource;
    private final BusinessContentRepository businessContentRepository;

    public BusinessContentCollectResult collect(String corpCode, Instant baseTime) {
        List<BusinessReportFiling> filings = businessReportSource.findRecent(corpCode, baseTime);
        if (filings.isEmpty()) {
            return BusinessContentCollectResult.NO_REPORT;
        }
        for (BusinessReportFiling filing : filings) {
            if (businessContentRepository.existsByFilingNumber(filing.filingNumber())) {
                return BusinessContentCollectResult.ALREADY_COLLECTED;
            }
            try {
                save(filing, businessReportSource.fetchBusinessContent(filing), baseTime);
                return BusinessContentCollectResult.COLLECTED;
            } catch (DisclosureDocumentMissingException e) {
                continue;
            }
        }
        throw new DisclosureSourceException("다운로드 가능한 사업보고서 원문 없음: " + corpCode);
    }

    private void save(BusinessReportFiling filing, String content, Instant baseTime) {
        businessContentRepository.save(BusinessContent.builder()
                .id(Ids.generate())
                .corpCode(filing.corpCode())
                .filingNumber(filing.filingNumber())
                .filedDate(filing.filedDate())
                .content(content)
                .collectedAt(baseTime)
                .build());
    }
}
