package dev.gukin.einvestlab.market.infrastructure.fsc;

import dev.gukin.einvestlab.global.config.FscApiProperties;
import dev.gukin.einvestlab.market.domain.DailyStockPriceEntry;
import dev.gukin.einvestlab.market.domain.DailyStockPriceSource;
import dev.gukin.einvestlab.market.domain.MarketSourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DailyStockPriceSourceAdapter implements DailyStockPriceSource {

    private static final int PAGE_SIZE = 1_000;
    private static final Duration PAGE_DELAY = Duration.ofSeconds(1);
    private static final DateTimeFormatter FSC_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final FscApiProperties properties;

    @Override
    public List<DailyStockPriceEntry> fetchAll(LocalDate tradeDate) {
        List<DailyStockPriceEntry> entries = new ArrayList<>();
        int page = 1;
        while (true) {
            StockPriceResponse response = readResponse(fetchPage(tradeDate, page));
            response.requireSuccess();
            List<DailyStockPriceEntry> pageEntries = response.toEntries();
            entries.addAll(pageEntries);
            if (pageEntries.isEmpty() || entries.size() >= response.totalCount()) {
                return entries;
            }
            page++;
            delay();
        }
    }

    private StockPriceResponse readResponse(String body) {
        try {
            return objectMapper.readValue(body, StockPriceResponse.class);
        } catch (JacksonException e) {
            throw new MarketSourceException("주식시세 응답 파싱 실패", e);
        }
    }

    private String fetchPage(LocalDate tradeDate, int page) {
        HttpRequest request = HttpRequest.newBuilder(buildPriceUri(tradeDate, page))
                .GET()
                .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new MarketSourceException("주식시세 HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new MarketSourceException("주식시세 요청 실패", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MarketSourceException("주식시세 요청 중단됨", e);
        }
    }

    URI buildPriceUri(LocalDate tradeDate, int page) {
        return URI.create(properties.baseUrl() + "/GetStockSecuritiesInfoService/getStockPriceInfo"
                + "?serviceKey=" + properties.key()
                + "&resultType=json"
                + "&basDt=" + FSC_DATE_FORMATTER.format(tradeDate)
                + "&numOfRows=" + PAGE_SIZE
                + "&pageNo=" + page);
    }

    private void delay() {
        try {
            Thread.sleep(PAGE_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MarketSourceException("페이지 간 대기 중단됨", e);
        }
    }
}
