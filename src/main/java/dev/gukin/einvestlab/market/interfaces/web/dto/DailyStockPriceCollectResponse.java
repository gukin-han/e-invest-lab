package dev.gukin.einvestlab.market.interfaces.web.dto;

import dev.gukin.einvestlab.market.application.DailyStockPriceCollectResult;

public record DailyStockPriceCollectResponse(int upsertedPrices, int tradedDays) {

    public static DailyStockPriceCollectResponse from(DailyStockPriceCollectResult result) {
        return new DailyStockPriceCollectResponse(result.upsertedPrices(), result.tradedDays());
    }
}
