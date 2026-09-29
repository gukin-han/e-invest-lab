package dev.gukin.einvestlab.market.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record PriceDistribution(
        List<Bin> bins,
        BigDecimal mean,
        int tradingDays,
        int minPrice,
        int maxPrice
) {

    private static final int[] NICE_UNITS = {1, 2, 5};
    private static final int MEAN_SCALE = 2;

    public record Bin(int from, int to, int count) {
    }

    public static Optional<PriceDistribution> of(List<DailyStockPrice> prices, int targetBins) {
        if (targetBins < 1) {
            throw new IllegalArgumentException("구간 개수는 1 이상이어야 한다: " + targetBins);
        }

        int[] closes = prices.stream()
                .mapToInt(DailyStockPrice::getClosePrice)
                .filter(close -> close > 0)
                .toArray();
        if (closes.length == 0) {
            return Optional.empty();
        }

        int min = Arrays.stream(closes).min().orElseThrow();
        int max = Arrays.stream(closes).max().orElseThrow();
        long total = Arrays.stream(closes).asLongStream().sum();

        int step = niceStep(max - min, targetBins);
        int start = min / step * step;

        int[] counts = new int[(max - start) / step + 1];
        for (int close : closes) {
            counts[(close - start) / step]++;
        }

        List<Bin> bins = new ArrayList<>(counts.length);
        for (int index = 0; index < counts.length; index++) {
            bins.add(new Bin(start + index * step, start + (index + 1) * step, counts[index]));
        }

        BigDecimal mean = BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(closes.length), MEAN_SCALE, RoundingMode.HALF_UP);

        return Optional.of(new PriceDistribution(List.copyOf(bins), mean, closes.length, min, max));
    }

    private static int niceStep(int span, int targetBins) {
        double rawWidth = Math.max((double) span / targetBins, 1);
        int magnitude = (int) Math.pow(10, (int) Math.floor(Math.log10(rawWidth)));
        for (int unit : NICE_UNITS) {
            if (unit * magnitude >= rawWidth) {
                return unit * magnitude;
            }
        }
        return 10 * magnitude;
    }
}
