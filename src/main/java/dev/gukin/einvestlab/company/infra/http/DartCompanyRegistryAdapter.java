package dev.gukin.einvestlab.company.infra.http;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.company.domain.CompanyRegistrySource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class DartCompanyRegistryAdapter implements CompanyRegistrySource {

    private final HttpClient httpClient;
    private final DartApiProperties properties;
    private final DartCompanyRegistryReader reader;

    @Override
    public void streamAll(Consumer<Company> handler) {
        try (InputStream zipBody = fetchCorpCodeZip()) {
            reader.read(zipBody, handler);
        } catch (IOException e) {
            throw new DartClientException("corpCode 응답 스트림 읽기 실패", e);
        }
    }

    private InputStream fetchCorpCodeZip() {
        HttpRequest request = HttpRequest.newBuilder(buildCorpCodeUri())
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new DartClientException("DART corpCode HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new DartClientException("DART corpCode 요청 실패", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DartClientException("DART corpCode 요청 중단됨", e);
        }
    }

    private URI buildCorpCodeUri() {
        return URI.create(properties.baseUrl() + "/corpCode.xml?crtfc_key=" + properties.key());
    }
}
