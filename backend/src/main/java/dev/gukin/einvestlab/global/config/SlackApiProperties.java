package dev.gukin.einvestlab.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack.webhook")
public record SlackApiProperties(String url) {
}
