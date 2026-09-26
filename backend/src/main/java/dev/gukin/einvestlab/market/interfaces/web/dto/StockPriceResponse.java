package dev.gukin.einvestlab.market.interfaces.web.dto;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;

import java.time.LocalDate;

public record StockPriceResponse(LocalDate tradeDate, int closePrice) {

    public static StockPriceResponse from(DailyStockPrice price) {
        return new StockPriceResponse(price.getTradeDate(), price.getClosePrice());
    }
}
