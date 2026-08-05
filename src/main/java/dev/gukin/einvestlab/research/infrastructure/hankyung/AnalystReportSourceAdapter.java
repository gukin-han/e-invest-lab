package dev.gukin.einvestlab.research.infrastructure.hankyung;

import dev.gukin.einvestlab.global.config.HankyungApiProperties;
import dev.gukin.einvestlab.research.domain.AnalystReportListing;
import dev.gukin.einvestlab.research.domain.AnalystReportSource;
import dev.gukin.einvestlab.research.domain.ResearchSourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
public class AnalystReportSourceAdapter implements AnalystReportSource {

    private static final int PAGE_SIZE = 80;
    private static final Duration PAGE_DELAY = Duration.ofSeconds(1);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final HttpClient httpClient;
    private final HankyungApiProperties properties;
    private final AnalystReportListReader reader;

    @Override
    public List<AnalystReportListing> fetchListings(LocalDate start, LocalDate end) {
        String firstPage = fetchPage(start, end, 1);
        List<AnalystReportListing> listings = new ArrayList<>(reader.readListings(firstPage));
        int lastPage = reader.readLastPage(firstPage);
        for (int page = 2; page <= lastPage; page++) {
            delay();
            listings.addAll(reader.readListings(fetchPage(start, end, page)));
        }
        return listings;
    }

    private String fetchPage(LocalDate start, LocalDate end, int page) {
        HttpRequest request = HttpRequest.newBuilder(buildListUri(start, end, page))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResearchSourceException("한경 컨센서스 목록 HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new ResearchSourceException("한경 컨센서스 목록 요청 실패", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResearchSourceException("한경 컨센서스 목록 요청 중단됨", e);
        }
    }

    URI buildListUri(LocalDate start, LocalDate end, int page) {
        return URI.create(properties.baseUrl() + "/analysis/list"
                + "?skinType=business"
                + "&sdate=" + DATE.format(start)
                + "&edate=" + DATE.format(end)
                + "&now_page=" + page
                + "&pagenum=" + PAGE_SIZE);
    }

    private void delay() {
        try {
            Thread.sleep(PAGE_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResearchSourceException("페이지 간 대기 중단됨", e);
        }
    }
}
