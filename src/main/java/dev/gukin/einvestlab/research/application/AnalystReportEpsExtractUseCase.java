package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.support.outbox.application.OutboxEventPublisher;
import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfStore;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.domain.EpsExtractedEvent;
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

    private static final int YEARS_BEFORE_PUBLISH = 10;
    private static final int YEARS_AFTER_PUBLISH = 5;

    private final AnalystReportRepository reportRepository;
    private final EpsEstimateRepository estimateRepository;
    private final AnalystReportPdfStore pdfStore;
    private final EpsExtractor epsExtractor;
    private final OutboxEventPublisher outboxPublisher;
    private final TransactionTemplate reportTransaction;

    public AnalystReportEpsExtractUseCase(AnalystReportRepository reportRepository,
                                          EpsEstimateRepository estimateRepository,
                                          AnalystReportPdfStore pdfStore,
                                          EpsExtractor epsExtractor,
                                          OutboxEventPublisher outboxPublisher,
                                          PlatformTransactionManager transactionManager) {
        this.reportRepository = reportRepository;
        this.estimateRepository = estimateRepository;
        this.pdfStore = pdfStore;
        this.epsExtractor = epsExtractor;
        this.outboxPublisher = outboxPublisher;
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
        if (!pdfStore.exists(report.getPdfPath())) {
            log.warn("PDF 파일 유실 — 재다운로드 대상으로 되돌림 (report_idx={}, path={})",
                    report.getReportIdx(), report.getPdfPath());
            reportTransaction.executeWithoutResult(tx -> {
                report.detachPdf();
                reportRepository.save(report);
            });
            return EpsExtractionStatus.FAILED;
        }
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
                estimateRepository.deleteAllByReportIdx(report.getReportIdx());
                estimateRepository.saveAll(extraction.figures().stream()
                        .map(figure -> toEstimate(report, figure, baseTime))
                        .toList());
                EpsExtractedEvent event = toEvent(report, extraction);
                outboxPublisher.publish(EpsExtractedEvent.TYPE,
                        String.valueOf(event.reportIdx()), event, baseTime);
            }
            report.recordEpsExtraction(extraction.status());
            reportRepository.save(report);
        });
    }

    private EpsExtractedEvent toEvent(AnalystReport report, EpsExtraction extraction) {
        return new EpsExtractedEvent(report.getReportIdx(), report.getStockCode(),
                report.getCompanyName(), extraction.figures());
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
