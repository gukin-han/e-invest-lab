package dev.gukin.einvestlab.research.infrastructure.slack;

import dev.gukin.einvestlab.research.domain.EpsFigure;
import dev.gukin.einvestlab.research.domain.EpsNotification;
import dev.gukin.einvestlab.research.domain.RevisionSignal;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class SlackEpsMessage {

    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0.##");
    private static final DecimalFormat RATE = new DecimalFormat("+0.0%;-0.0%");
    private static final String INDENT = "　　";
    private static final Map<RevisionSignal, String> SIGNAL_LABELS = Map.of(
            RevisionSignal.NEW, "신규",
            RevisionSignal.STRONG_UP, "강한 상향",
            RevisionSignal.UP, "상향",
            RevisionSignal.FLAT, "유지",
            RevisionSignal.DOWN, "하향",
            RevisionSignal.STRONG_DOWN, "강한 하향");

    private final EpsNotification n;
    private final String hankyungBaseUrl;

    SlackEpsMessage(EpsNotification notification, String hankyungBaseUrl) {
        this.n = notification;
        this.hankyungBaseUrl = hankyungBaseUrl;
    }

    String render() {
        List<String> lines = new ArrayList<>();
        lines.add(headline());
        lines.addAll(keyEpsLines());
        targetPriceLine().ifPresent(lines::add);
        priceLine().ifPresent(lines::add);
        lines.add("");
        lines.add("EPS 경로");
        n.figures().forEach(figure -> lines.add(pathLine(figure)));
        lines.add("리포트: " + hankyungBaseUrl + "/analysis/downpdf?report_idx=" + n.reportIdx());
        return String.join("\n", lines);
    }

    private String headline() {
        return "*[" + SIGNAL_LABELS.get(n.signal()) + "] " + n.companyName() + " (" + n.stockCode() + ")*"
                + " · " + n.broker() + " · " + shortDate(n.publishedDate());
    }

    private List<String> keyEpsLines() {
        List<String> lines = new ArrayList<>();
        StringBuilder head = new StringBuilder().append(n.keyYear()).append("E EPS");
        n.keyFigure().ifPresent(f -> head.append("  ").append(number(f.eps())));
        lines.add(head.toString());
        n.previousKeyEps().ifPresent(prev -> {
            StringBuilder line = new StringBuilder(INDENT).append("직전 ").append(number(prev));
            n.revisionRate().ifPresent(rate -> line.append(" → ").append(rate(rate)));
            lines.add(line.toString());
        });
        n.consensusKeyEps().ifPresent(avg -> {
            StringBuilder line = new StringBuilder(INDENT).append("컨센서스 ").append(number(avg));
            n.consensusGapRate().ifPresent(rate -> line.append(" 대비 ").append(rate(rate)));
            lines.add(line.toString());
        });
        return lines;
    }

    private Optional<String> targetPriceLine() {
        List<String> parts = new ArrayList<>();
        if (n.hasTargetPrice()) {
            StringBuilder tp = new StringBuilder("목표주가  ");
            if (n.targetPriceUnchanged()) {
                tp.append(number(n.targetPrice())).append(" 유지");
            } else {
                n.previousTargetPrice().ifPresent(prev -> tp.append(number(prev)).append(" → "));
                tp.append(number(n.targetPrice()));
                n.targetPriceChangeRate().ifPresent(rate -> tp.append(" (").append(rate(rate)).append(")"));
            }
            parts.add(tp.toString());
        }
        if (n.opinion() != null && !n.opinion().isBlank()) {
            parts.add(parts.isEmpty() ? "투자의견  " + n.opinion() : n.opinion());
        }
        return parts.isEmpty() ? Optional.empty() : Optional.of(String.join(" · ", parts));
    }

    private Optional<String> priceLine() {
        if (n.latestPrice() == null || n.latestPrice().closePrice() <= 0) {
            return Optional.empty();
        }
        StringBuilder line = new StringBuilder("현재가  ")
                .append(number(n.latestPrice().closePrice()))
                .append(" (").append(shortDate(n.latestPrice().tradeDate())).append(")");
        n.forwardPer().ifPresent(per -> line.append(" · PER ").append(per.toPlainString()).append("배 (")
                .append(n.broker()).append(" ").append(n.keyYear()).append("E 기준)"));
        return Optional.of(line.toString());
    }

    private String pathLine(EpsFigure figure) {
        StringBuilder line = new StringBuilder()
                .append(figure.fiscalYear()).append(figure.estimated() ? "E" : "A")
                .append("  ").append(number(figure.eps()));
        n.yearOverYearRate(figure).ifPresent(rate -> line.append("  ").append(rate(rate)));
        return line.toString();
    }

    private static String shortDate(LocalDate date) {
        return date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    private static String number(BigDecimal value) {
        return NUMBER.format(value);
    }

    private static String number(long value) {
        return NUMBER.format(value);
    }

    private static String rate(BigDecimal percent) {
        return RATE.format(percent.movePointLeft(2));
    }
}
