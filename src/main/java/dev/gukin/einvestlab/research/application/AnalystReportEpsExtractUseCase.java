package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfStore;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.domain.EpsExtraction;
import dev.gukin.einvestlab.research.domain.EpsExtractionStatus;
import dev.gukin.einvestlab.research.domain.EpsExtractor;
import dev.gukin.einvestlab.research.domain.EpsFigure;
import dev.gukin.einvestlab.research.domain.PdfTextExtractionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class AnalystReportEpsExtractUseCase {

    private static final int YEARS_BEFORE_PUBLISH = 3;
    private static final int YEARS_AFTER_PUBLISH = 5;

    private final AnalystReportRepository reportRepository;
    private final EpsEstimateRepository estimateRepository;
    private final AnalystReportPdfStore pdfStore;
    private final EpsExtractor epsExtractor;
    private final TransactionTemplate reportTransaction;

    public AnalystReportEpsExtractUseCase(AnalystReportRepository reportRepository,
                                          EpsEstimateRepository estimateRepository,
                                          AnalystReportPdfStore pdfStore,
                                          EpsExtractor epsExtractor,
                                          PlatformTransactionManager transactionManager) {
        this.reportRepository = reportRepository;
        this.estimateRepository = estimateRepository;
        this.pdfStore = pdfStore;
        this.epsExtractor = epsExtractor;
        this.reportTransaction = new TransactionTemplate(transactionManager);
    }

    public AnalystReportEpsExtractResult extractAll(Instant baseTime) {
        int extracted = 0;
        int noSummaryTable = 0;
        int failed = 0;
        for (AnalystReport report : reportRepository.findAllPendingEpsExtraction()) {
            switch (extractOne(report, baseTime)) {
                case EXTRACTED -> extracted++;
                case NO_SUMMARY_TABLE -> noSummaryTable++;
                case FAILED -> failed++;
            }
        }
        return new AnalystReportEpsExtractResult(extracted, noSummaryTable, failed);
    }

    private EpsExtractionStatus extractOne(AnalystReport report, Instant baseTime) {
        EpsExtraction extraction;
        try {
            extraction = applyGuards(epsExtractor.extract(pdfStore.resolve(report.getPdfPath())), report);
        } catch (PdfTextExtractionException e) {
            log.warn("EPS 추출 실패 (report_idx={}): {}", report.getReportIdx(), e.getMessage());
            extraction = EpsExtraction.failed();
        }
        save(report, extraction, baseTime);
        return extraction.status();
    }

    private EpsExtraction applyGuards(EpsExtraction extraction, AnalystReport report) {
        if (extraction.status() != EpsExtractionStatus.EXTRACTED) {
            return extraction;
        }
        int publishedYear = report.getPublishedDate().getYear();
        Set<Integer> seenYears = new HashSet<>();
        for (EpsFigure figure : extraction.figures()) {
            if (figure.fiscalYear() < publishedYear - YEARS_BEFORE_PUBLISH
                    || figure.fiscalYear() > publishedYear + YEARS_AFTER_PUBLISH) {
                log.warn("EPS 연도 범위 밖 (report_idx={}, fiscal_year={})",
                        report.getReportIdx(), figure.fiscalYear());
                return EpsExtraction.failed();
            }
            if (!seenYears.add(figure.fiscalYear())) {
                log.warn("EPS 연도 중복 (report_idx={}, fiscal_year={})",
                        report.getReportIdx(), figure.fiscalYear());
                return EpsExtraction.failed();
            }
        }
        return extraction;
    }

    private void save(AnalystReport report, EpsExtraction extraction, Instant baseTime) {
        reportTransaction.executeWithoutResult(tx -> {
            if (extraction.status() == EpsExtractionStatus.EXTRACTED) {
                estimateRepository.saveAll(extraction.figures().stream()
                        .map(figure -> toEstimate(report, figure, baseTime))
                        .toList());
            }
            report.recordEpsExtraction(extraction.status());
            reportRepository.save(report);
        });
    }

    private EpsEstimate toEstimate(AnalystReport report, EpsFigure figure, Instant baseTime) {
        return EpsEstimate.builder()
                .id(Ids.generate())
                .reportIdx(report.getReportIdx())
                .fiscalYear(figure.fiscalYear())
                .estimated(figure.estimated())
                .eps(figure.eps())
                .extractedAt(baseTime)
                .build();
    }
}
