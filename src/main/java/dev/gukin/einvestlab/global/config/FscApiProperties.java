package dev.gukin.einvestlab.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fsc.api")
public record FscApiProperties(String baseUrl, String key) {
}
