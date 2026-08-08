package dev.gukin.einvestlab.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "offering.extraction")
public record OfferingExtractionProperties(List<String> models) {
}
