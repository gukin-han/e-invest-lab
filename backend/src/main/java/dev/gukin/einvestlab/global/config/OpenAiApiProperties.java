package dev.gukin.einvestlab.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai.api")
public record OpenAiApiProperties(String baseUrl, String key) {
}
