package dev.gukin.einvestlab.disclosure.infra.dart;

import dev.gukin.einvestlab.disclosure.domain.BusinessReportFiling;
import dev.gukin.einvestlab.disclosure.domain.BusinessReportSource;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import dev.gukin.einvestlab.global.config.DartApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BusinessReportAdapter implements BusinessReportSource {

    private static final String ANNUAL_REPORT_TYPE = "A001";
    private static final Period SEARCH_RANGE = Period.ofYears(2);
    private static final DateTimeFormatter DART_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final HttpClient httpClient;
    private final DartApiProperties properties;
    private final ObjectMapper objectMapper;
    private final DocumentReader documentReader;
    private final BusinessContentExtractor businessContentExtractor;

    @Override
    public List<BusinessReportFiling> findRecent(String corpCode, Instant baseTime) {
        try (InputStream body = fetch(buildListUri(corpCode, baseTime), "DART 공시검색")) {
            return parse(body).toFilings(corpCode);
        } catch (IOException e) {
            throw new DisclosureSourceException("DART 공시검색 응답 스트림 읽기 실패", e);
        }
    }

    @Override
    public String fetchBusinessContent(BusinessReportFiling filing) {
        try (InputStream body = fetch(buildDocumentUri(filing.filingNumber()), "DART 원문 다운로드")) {
            String document = documentReader.readBody(filing.filingNumber(), body);
            return businessContentExtractor.extract(document);
        } catch (IOException e) {
            throw new DisclosureSourceException("DART 원문 응답 스트림 읽기 실패", e);
        }
    }

    private DisclosureSearchResponse parse(InputStream body) {
        try {
            return objectMapper.readValue(body, DisclosureSearchResponse.class);
        } catch (JacksonException e) {
            throw new DisclosureSourceException("DART 공시검색 응답 파싱 실패", e);
        }
    }

    private InputStream fetch(URI uri, String apiName) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new DisclosureSourceException(apiName + " HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new DisclosureSourceException(apiName + " 요청 실패", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DisclosureSourceException(apiName + " 요청 중단됨", e);
        }
    }

    URI buildListUri(String corpCode, Instant baseTime) {
        LocalDate endDate = baseTime.atZone(KOREA).toLocalDate();
        LocalDate beginDate = endDate.minus(SEARCH_RANGE);
        return URI.create(properties.baseUrl() + "/list.json"
                + "?crtfc_key=" + properties.key()
                + "&corp_code=" + corpCode
                + "&pblntf_detail_ty=" + ANNUAL_REPORT_TYPE
                + "&bgn_de=" + DART_DATE.format(beginDate)
                + "&end_de=" + DART_DATE.format(endDate)
                + "&page_count=100");
    }

    URI buildDocumentUri(String filingNumber) {
        return URI.create(properties.baseUrl() + "/document.xml"
                + "?crtfc_key=" + properties.key()
                + "&rcept_no=" + filingNumber);
    }
}
