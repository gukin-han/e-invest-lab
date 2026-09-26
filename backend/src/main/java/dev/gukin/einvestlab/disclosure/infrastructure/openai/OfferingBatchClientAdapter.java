package dev.gukin.einvestlab.disclosure.infrastructure.openai;

import dev.gukin.einvestlab.disclosure.domain.OfferingBatchClient;
import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionException;
import dev.gukin.einvestlab.global.config.OpenAiApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferingBatchClientAdapter implements OfferingBatchClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);
    private static final Set<String> TERMINAL_FAILURE_STATUSES = Set.of("failed", "expired", "cancelled");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiApiProperties properties;
    private final OfferingChatRequests chatRequests;

    @Override
    public String submit(Map<String, String> slicesByFilingNumber, String model) {
        String jsonl = buildJsonl(slicesByFilingNumber, model);
        String fileId = uploadBatchFile(jsonl);
        return createBatch(fileId);
    }

    @Override
    public BatchOutcome fetchOutcome(String providerBatchId) {
        JsonNode batch = readJson(send(get("/batches/" + providerBatchId)));
        String status = batch.path("status").asString("");
        if (TERMINAL_FAILURE_STATUSES.contains(status)) {
            log.warn("배치 실패 상태 (batch={}, status={})", providerBatchId, status);
            return BatchOutcome.failed();
        }
        if (!"completed".equals(status)) {
            return BatchOutcome.inProgress();
        }
        String outputFileId = batch.path("output_file_id").asString(null);
        if (outputFileId == null) {
            return BatchOutcome.failed();
        }
        return parseResults(send(get("/files/" + outputFileId + "/content")));
    }

    private String buildJsonl(Map<String, String> slicesByFilingNumber, String model) {
        StringBuilder jsonl = new StringBuilder();
        for (Map.Entry<String, String> entry : slicesByFilingNumber.entrySet()) {
            Map<String, Object> line = Map.of(
                    "custom_id", entry.getKey(),
                    "method", "POST",
                    "url", "/v1/chat/completions",
                    "body", chatRequests.chatBody(model, entry.getValue()));
            jsonl.append(objectMapper.writeValueAsString(line)).append('\n');
        }
        return jsonl.toString();
    }

    private String uploadBatchFile(String jsonl) {
        String boundary = "batch-" + System.nanoTime();
        byte[] body = multipartBody(boundary, jsonl);
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/files"))
                .header("Authorization", "Bearer " + properties.key())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return readJson(send(request)).path("id").asString(null);
    }

    private byte[] multipartBody(String boundary, String jsonl) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String head = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"purpose\"\r\n\r\nbatch\r\n"
                    + "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"offerings.jsonl\"\r\n"
                    + "Content-Type: application/jsonl\r\n\r\n";
            out.write(head.getBytes(StandardCharsets.UTF_8));
            out.write(jsonl.getBytes(StandardCharsets.UTF_8));
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String createBatch(String inputFileId) {
        if (inputFileId == null) {
            throw new OfferingExtractionException("배치 입력 파일 업로드 실패");
        }
        String body = objectMapper.writeValueAsString(Map.of(
                "input_file_id", inputFileId,
                "endpoint", "/v1/chat/completions",
                "completion_window", "24h"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/batches"))
                .header("Authorization", "Bearer " + properties.key())
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        String batchId = readJson(send(request)).path("id").asString(null);
        if (batchId == null) {
            throw new OfferingExtractionException("배치 생성 응답에 id 없음");
        }
        return batchId;
    }

    private BatchOutcome parseResults(String content) {
        Map<String, List<OfferingDraft>> drafts = new HashMap<>();
        List<String> failed = new ArrayList<>();
        for (String line : content.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(line);
                String filingNumber = node.path("custom_id").asString(null);
                if (filingNumber == null) {
                    continue;
                }
                JsonNode response = node.path("response");
                if (response.path("status_code").asInt(0) != 200) {
                    failed.add(filingNumber);
                    continue;
                }
                ChatCompletionResponse completion = objectMapper.treeToValue(
                        response.path("body"), ChatCompletionResponse.class);
                drafts.put(filingNumber, objectMapper
                        .readValue(completion.firstContent(), OfferingPayload.class).toDrafts());
            } catch (JacksonException | OfferingExtractionException e) {
                log.warn("배치 결과 행 해석 실패: {}", e.getMessage());
            }
        }
        return new BatchOutcome(BatchOutcome.State.COMPLETED, drafts, failed);
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(URI.create(properties.baseUrl() + path))
                .header("Authorization", "Bearer " + properties.key())
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
    }

    private String send(HttpRequest request) {
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

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JacksonException e) {
            throw new OfferingExtractionException("OpenAI 응답 파싱 실패", e);
        }
    }

    private static String truncate(String body) {
        return body == null ? "" : body.substring(0, Math.min(body.length(), 300));
    }
}
