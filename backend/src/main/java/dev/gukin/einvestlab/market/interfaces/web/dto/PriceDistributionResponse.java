package dev.gukin.einvestlab.market.interfaces.web.dto;

import dev.gukin.einvestlab.market.domain.PriceDistribution;

import java.math.BigDecimal;
import java.util.List;

public record PriceDistributionResponse(
        List<BinResponse> bins,
        BigDecimal mean,
        int tradingDays,
        int minPrice,
        int maxPrice
) {

    public record BinResponse(int from, int to, int count) {

        static BinResponse from(PriceDistribution.Bin bin) {
            return new BinResponse(bin.from(), bin.to(), bin.count());
        }
    }

    public static PriceDistributionResponse from(PriceDistribution distribution) {
        return new PriceDistributionResponse(
                distribution.bins().stream().map(BinResponse::from).toList(),
                distribution.mean(),
                distribution.tradingDays(),
                distribution.minPrice(),
                distribution.maxPrice());
    }
}
