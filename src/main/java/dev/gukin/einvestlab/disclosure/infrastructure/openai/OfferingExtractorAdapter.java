package dev.gukin.einvestlab.disclosure.infrastructure.openai;

import dev.gukin.einvestlab.global.config.OpenAiApiProperties;
import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionException;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OfferingExtractorAdapter implements OfferingExtractor {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);
    private static final String SYSTEM_PROMPT_RESOURCE = "/prompts/offering-extraction-system.txt";
    private static final String SCHEMA_RESOURCE = "/prompts/offering-schema.json";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiApiProperties properties;
    private final String systemPrompt;
    private final JsonNode outputSchema;

    public OfferingExtractorAdapter(HttpClient httpClient, ObjectMapper objectMapper,
                                    OpenAiApiProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.systemPrompt = readResource(SYSTEM_PROMPT_RESOURCE);
        this.outputSchema = objectMapper.readTree(readResource(SCHEMA_RESOURCE));
    }

    @Override
    public List<OfferingDraft> extract(String slicedContent, String model) {
        String responseBody = send(buildRequestBody(model, slicedContent));
        try {
            ChatCompletionResponse response =
                    objectMapper.readValue(responseBody, ChatCompletionResponse.class);
            return objectMapper.readValue(response.firstContent(), OfferingPayload.class).toDrafts();
        } catch (JacksonException e) {
            throw new OfferingExtractionException("OpenAI 응답 파싱 실패", e);
        }
    }

    private String buildRequestBody(String model, String slicedContent) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", slicedContent)),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "offering_extraction",
                                "strict", true,
                                "schema", outputSchema)));
        return objectMapper.writeValueAsString(body);
    }

    private String send(String requestBody) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/chat/completions"))
                .header("Authorization", "Bearer " + properties.key())
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new OfferingExtractionException("OpenAI HTTP " + response.statusCode()
                        + ": " + truncate(response.body()));
            }
            return response.body();
        } catch (IOException e) {
            throw new OfferingExtractionException("OpenAI 요청 실패", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OfferingExtractionException("OpenAI 요청 중단됨", e);
        }
    }

    private static String truncate(String body) {
        return body == null ? "" : body.substring(0, Math.min(body.length(), 300));
    }

    private static String readResource(String path) {
        try (InputStream input = OfferingExtractorAdapter.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("리소스 없음: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("리소스 읽기 실패: " + path, e);
        }
    }
}
