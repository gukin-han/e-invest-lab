package dev.gukin.einvestlab.research.infrastructure.slack;

import dev.gukin.einvestlab.research.domain.EpsFigure;
import dev.gukin.einvestlab.research.domain.EpsNotification;
import dev.gukin.einvestlab.research.domain.RevisionSignal;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SlackEpsMessage {

    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0.##");
    private static final DecimalFormat RATE = new DecimalFormat("+0.0%;-0.0%");
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
        lines.add(keyEpsLine());
        String priceLine = priceLine();
        if (!priceLine.isEmpty()) {
            lines.add(priceLine);
        }
        lines.add("");
        lines.add("EPS 경로");
        lines.add("```");
        n.figures().forEach(figure -> lines.add(pathLine(figure)));
        lines.add("```");
        lines.add("리포트: " + hankyungBaseUrl + "/analysis/downpdf?report_idx=" + n.reportIdx());
        return String.join("\n", lines);
    }

    private String headline() {
        return "*[" + SIGNAL_LABELS.get(n.signal()) + "] " + n.companyName() + " (" + n.stockCode() + ")*"
                + " — " + n.broker() + " · " + n.publishedDate();
    }

    private String keyEpsLine() {
        StringBuilder line = new StringBuilder().append(n.keyYear()).append("E EPS ");
        n.keyFigure().ifPresent(f -> line.append(number(f.eps())));
        n.previousKeyEps().ifPresent(prev -> {
            line.append(" (직전 ").append(number(prev));
            n.revisionRate().ifPresent(rate -> line.append(", ").append(rate(rate)));
            line.append(")");
        });
        n.consensusKeyEps().ifPresent(avg -> {
            line.append(" · 컨센서스 ").append(number(avg));
            n.consensusGapRate().ifPresent(rate -> line.append(" 대비 ").append(rate(rate)));
        });
        return line.toString();
    }

    private String priceLine() {
        List<String> parts = new ArrayList<>();
        if (n.hasTargetPrice()) {
            StringBuilder tp = new StringBuilder("목표주가 ");
            n.previousTargetPrice().ifPresent(prev -> tp.append(number(prev)).append(" → "));
            tp.append(number(n.targetPrice()));
            n.targetPriceChangeRate().ifPresent(rate -> tp.append(" (").append(rate(rate)).append(")"));
            parts.add(tp.toString());
        }
        if (n.opinion() != null && !n.opinion().isBlank()) {
            parts.add("투자의견 " + n.opinion());
        }
        if (n.closePrice() != null && n.closePrice() > 0) {
            StringBuilder price = new StringBuilder("현재가 ").append(number(n.closePrice()));
            n.forwardPer().ifPresent(per -> price.append(" (")
                    .append(n.keyYear()).append("E PER ").append(per.toPlainString()).append("배)"));
            parts.add(price.toString());
        }
        return String.join(" · ", parts);
    }

    private String pathLine(EpsFigure figure) {
        String label = figure.fiscalYear() + (figure.estimated() ? "E" : "A");
        String eps = number(figure.eps());
        String yoy = n.yearOverYearRate(figure).map(SlackEpsMessage::rate).orElse("");
        return String.format("%-6s %10s %9s", label, eps, yoy).stripTrailing();
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
