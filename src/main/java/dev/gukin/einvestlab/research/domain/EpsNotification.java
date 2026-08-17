package dev.gukin.einvestlab.research.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record EpsNotification(
        long reportIdx,
        String stockCode,
        String companyName,
        String broker,
        LocalDate publishedDate,
        String opinion,
        Long targetPrice,
        List<EpsFigure> figures,
        PreviousReport previous,
        List<EpsConsensus> consensus,
        Integer closePrice
) {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public record PreviousReport(long reportIdx, LocalDate publishedDate, Long targetPrice,
                                 String opinion, List<EpsFigure> figures) {
    }

    public EpsNotification {
        figures = figures.stream().sorted(Comparator.comparingInt(EpsFigure::fiscalYear)).toList();
    }

    public int keyYear() {
        return figures.stream()
                .filter(EpsFigure::estimated)
                .findFirst()
                .map(EpsFigure::fiscalYear)
                .orElseGet(() -> figures.getLast().fiscalYear());
    }

    public Optional<EpsFigure> keyFigure() {
        return figureOf(figures, keyYear());
    }

    public Optional<BigDecimal> previousKeyEps() {
        if (previous == null) {
            return Optional.empty();
        }
        return figureOf(previous.figures(), keyYear()).map(EpsFigure::eps);
    }

    public Optional<BigDecimal> revisionRate() {
        return keyFigure().flatMap(current ->
                previousKeyEps().flatMap(prev -> changeRate(current.eps(), prev)));
    }

    public RevisionSignal signal() {
        return RevisionSignal.of(revisionRate().orElse(null));
    }

    public Optional<BigDecimal> consensusKeyEps() {
        return consensus.stream()
                .filter(c -> c.fiscalYear() == keyYear())
                .map(EpsConsensus::averageEps)
                .findFirst();
    }

    public Optional<BigDecimal> consensusGapRate() {
        return keyFigure().flatMap(current ->
                consensusKeyEps().flatMap(avg -> changeRate(current.eps(), avg)));
    }

    public boolean hasTargetPrice() {
        return targetPrice != null && targetPrice > 0;
    }

    public Optional<Long> previousTargetPrice() {
        if (previous == null || previous.targetPrice() == null || previous.targetPrice() <= 0) {
            return Optional.empty();
        }
        return Optional.of(previous.targetPrice());
    }

    public Optional<BigDecimal> targetPriceChangeRate() {
        if (!hasTargetPrice()) {
            return Optional.empty();
        }
        return previousTargetPrice().flatMap(prev ->
                changeRate(BigDecimal.valueOf(targetPrice), BigDecimal.valueOf(prev)));
    }

    public Optional<BigDecimal> forwardPer() {
        if (closePrice == null || closePrice <= 0) {
            return Optional.empty();
        }
        return keyFigure()
                .filter(f -> f.eps().signum() > 0)
                .map(f -> BigDecimal.valueOf(closePrice).divide(f.eps(), 1, RoundingMode.HALF_UP));
    }

    public Optional<BigDecimal> yearOverYearRate(EpsFigure figure) {
        return figureOf(figures, figure.fiscalYear() - 1)
                .flatMap(prev -> changeRate(figure.eps(), prev.eps()));
    }

    static Optional<BigDecimal> changeRate(BigDecimal current, BigDecimal base) {
        if (base == null || base.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(current.subtract(base)
                .multiply(HUNDRED)
                .divide(base.abs(), 1, RoundingMode.HALF_UP));
    }

    private static Optional<EpsFigure> figureOf(List<EpsFigure> figures, int fiscalYear) {
        return figures.stream().filter(f -> f.fiscalYear() == fiscalYear).findFirst();
    }
}
