package dev.gukin.einvestlab.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DART OpenAPI 접속 설정. application.yml 의 {@code dart.api.*} 에 바인딩.
 */
@ConfigurationProperties(prefix = "dart.api")
public record DartApiProperties(String baseUrl, String key) {
}
