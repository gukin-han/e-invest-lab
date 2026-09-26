package dev.gukin.einvestlab.disclosure.infrastructure.openai;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
class OfferingChatRequests {

    private static final String SYSTEM_PROMPT_RESOURCE = "/prompts/offering-extraction-system.txt";
    private static final String SCHEMA_RESOURCE = "/prompts/offering-schema.json";

    private final String systemPrompt;
    private final JsonNode outputSchema;

    OfferingChatRequests(ObjectMapper objectMapper) {
        this.systemPrompt = readResource(SYSTEM_PROMPT_RESOURCE);
        this.outputSchema = objectMapper.readTree(readResource(SCHEMA_RESOURCE));
    }

    Map<String, Object> chatBody(String model, String slicedContent) {
        return Map.of(
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
    }

    private static String readResource(String path) {
        try (InputStream input = OfferingChatRequests.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("리소스 없음: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("리소스 읽기 실패: " + path, e);
        }
    }
}
