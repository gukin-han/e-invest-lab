package dev.gukin.einvestlab.research.infrastructure.slack;

import dev.gukin.einvestlab.global.config.HankyungApiProperties;
import dev.gukin.einvestlab.global.config.SlackApiProperties;
import dev.gukin.einvestlab.research.domain.EpsNotification;
import dev.gukin.einvestlab.research.domain.EpsNotificationException;
import dev.gukin.einvestlab.research.domain.EpsNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SlackEpsNotifierAdapter implements EpsNotifier {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SlackApiProperties properties;
    private final HankyungApiProperties hankyungProperties;

    @Override
    public void notify(EpsNotification notification) {
        if (properties.url() == null || properties.url().isBlank()) {
            throw new EpsNotificationException("슬랙 웹훅 URL 미설정 (SLACK_WEBHOOK_URL)");
        }
        String text = new SlackEpsMessage(notification, hankyungProperties.baseUrl()).render();
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.url()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(Map.of("text", text))))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new EpsNotificationException("슬랙 웹훅 HTTP " + response.statusCode()
                        + " (report_idx=" + notification.reportIdx() + ", body=" + response.body() + ")");
            }
        } catch (IOException e) {
            throw new EpsNotificationException(
                    "슬랙 웹훅 요청 실패 (report_idx=" + notification.reportIdx() + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EpsNotificationException("슬랙 웹훅 요청 중단됨", e);
        }
    }
}
