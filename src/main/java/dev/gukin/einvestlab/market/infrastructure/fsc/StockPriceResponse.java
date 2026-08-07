package dev.gukin.einvestlab.market.infrastructure.fsc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.gukin.einvestlab.market.domain.DailyStockPriceEntry;
import dev.gukin.einvestlab.market.domain.MarketSourceException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record StockPriceResponse(@JsonProperty("response") Payload payload) {

    private static final String SUCCESS_CODE = "00";
    private static final DateTimeFormatter FSC_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Payload(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Header(
            @JsonProperty("resultCode") String resultCode,
            @JsonProperty("resultMsg") String resultMessage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Body(
            @JsonProperty("totalCount") int totalCount,
            @JsonProperty("items") Items items
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Items(@JsonProperty("item") List<Item> item) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(
            @JsonProperty("basDt") String baseDate,
            @JsonProperty("srtnCd") String stockCode,
            @JsonProperty("mrktCtg") String marketCategory,
            @JsonProperty("mkp") String openPrice,
            @JsonProperty("hipr") String highPrice,
            @JsonProperty("lopr") String lowPrice,
            @JsonProperty("clpr") String closePrice,
            @JsonProperty("trqu") String volume
    ) {
    }

    void requireSuccess() {
        if (payload == null || payload.header() == null) {
            throw new MarketSourceException("주식시세 응답 형식 오류 (서비스 키 미등록 가능성)");
        }
        if (!SUCCESS_CODE.equals(payload.header().resultCode())) {
            throw new MarketSourceException("주식시세 응답 오류 [" + payload.header().resultCode()
                    + "] " + payload.header().resultMessage());
        }
    }

    int totalCount() {
        return payload.body().totalCount();
    }

    List<DailyStockPriceEntry> toEntries() {
        if (payload.body().items() == null || payload.body().items().item() == null) {
            return List.of();
        }
        return payload.body().items().item().stream()
                .map(StockPriceResponse::toEntry)
                .toList();
    }

    private static DailyStockPriceEntry toEntry(Item item) {
        return new DailyStockPriceEntry(
                item.stockCode(),
                LocalDate.parse(item.baseDate(), FSC_DATE_FORMATTER),
                item.marketCategory(),
                Integer.parseInt(item.openPrice()),
                Integer.parseInt(item.highPrice()),
                Integer.parseInt(item.lowPrice()),
                Integer.parseInt(item.closePrice()),
                Long.parseLong(item.volume()));
    }
}
