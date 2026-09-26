package dev.gukin.einvestlab.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hankyung.api")
public record HankyungApiProperties(String baseUrl) {
}
