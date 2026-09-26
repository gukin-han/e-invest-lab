package dev.gukin.einvestlab.disclosure.infrastructure.openai;

import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionException;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractor;
import dev.gukin.einvestlab.global.config.OpenAiApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OfferingExtractorAdapter implements OfferingExtractor {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiApiProperties properties;
    private final OfferingChatRequests chatRequests;

    @Override
    public List<OfferingDraft> extract(String slicedContent, String model) {
        String requestBody = objectMapper.writeValueAsString(chatRequests.chatBody(model, slicedContent));
        String responseBody = send(requestBody);
        try {
            ChatCompletionResponse response =
                    objectMapper.readValue(responseBody, ChatCompletionResponse.class);
            return objectMapper.readValue(response.firstContent(), OfferingPayload.class).toDrafts();
        } catch (JacksonException e) {
            throw new OfferingExtractionException("OpenAI 응답 파싱 실패", e);
        }
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
}
