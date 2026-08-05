package dev.gukin.einvestlab.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "research.pdf-storage")
public record PdfStorageProperties(String root) {
}
