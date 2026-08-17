package dev.gukin.einvestlab.research.application;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import dev.gukin.einvestlab.research.domain.AnalystReport;
import dev.gukin.einvestlab.research.domain.AnalystReportRepository;
import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.domain.EpsExtractedEvent;
import dev.gukin.einvestlab.research.domain.EpsFigure;
import dev.gukin.einvestlab.research.domain.EpsNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EpsNotificationComposer {

    private final AnalystReportRepository reportRepository;
    private final EpsEstimateRepository estimateRepository;
    private final DailyStockPriceRepository priceRepository;

    public EpsNotification compose(EpsExtractedEvent event) {
        AnalystReport report = reportRepository.findByReportIdx(event.reportIdx())
                .orElseThrow(() -> new IllegalStateException("리포트 없음 (report_idx=" + event.reportIdx() + ")"));
        EpsNotification.PreviousReport previous = reportRepository
                .findPreviousExtractedByBroker(report.getStockCode(), report.getBroker(),
                        report.getPublishedDate(), report.getReportIdx())
                .map(this::toPrevious)
                .orElse(null);
        return new EpsNotification(
                report.getReportIdx(), report.getStockCode(), report.getCompanyName(),
                report.getBroker(), report.getPublishedDate(), report.getOpinion(), report.getTargetPrice(),
                event.figures(),
                previous,
                estimateRepository.findConsensus(report.getStockCode(),
                        report.getPublishedDate().minusMonths(EpsStatisticsQuery.VALID_MONTHS),
                        report.getBroker()),
                priceRepository.findLatestByStockCode(report.getStockCode())
                        .map(DailyStockPrice::getClosePrice)
                        .orElse(null));
    }

    private EpsNotification.PreviousReport toPrevious(AnalystReport previous) {
        List<EpsFigure> figures = estimateRepository.findAllByReportIdx(previous.getReportIdx()).stream()
                .map(e -> new EpsFigure(e.getFiscalYear(), e.isEstimated(), e.getEps()))
                .toList();
        return new EpsNotification.PreviousReport(previous.getReportIdx(), previous.getPublishedDate(),
                previous.getTargetPrice(), previous.getOpinion(), figures);
    }
}
