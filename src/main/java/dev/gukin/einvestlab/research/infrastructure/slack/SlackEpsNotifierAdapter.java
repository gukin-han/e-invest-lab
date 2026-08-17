package dev.gukin.einvestlab.research.infrastructure.slack;

import dev.gukin.einvestlab.global.config.SlackApiProperties;
import dev.gukin.einvestlab.research.domain.EpsExtractedEvent;
import dev.gukin.einvestlab.research.domain.EpsFigure;
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
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SlackEpsNotifierAdapter implements EpsNotifier {

    private static final DecimalFormat EPS_FORMAT = new DecimalFormat("#,##0.##");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SlackApiProperties properties;

    @Override
    public void notify(EpsExtractedEvent event) {
        if (properties.url() == null || properties.url().isBlank()) {
            throw new EpsNotificationException("슬랙 웹훅 URL 미설정 (SLACK_WEBHOOK_URL)");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.url()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(Map.of("text", buildMessage(event)))))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new EpsNotificationException("슬랙 웹훅 HTTP " + response.statusCode()
                        + " (report_idx=" + event.reportIdx() + ", body=" + response.body() + ")");
            }
        } catch (IOException e) {
            throw new EpsNotificationException("슬랙 웹훅 요청 실패 (report_idx=" + event.reportIdx() + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EpsNotificationException("슬랙 웹훅 요청 중단됨", e);
        }
    }

    static String buildMessage(EpsExtractedEvent event) {
        StringBuilder text = new StringBuilder()
                .append("*EPS 추출* ").append(event.companyName())
                .append(" (").append(event.stockCode()).append(")")
                .append(" — report_idx=").append(event.reportIdx());
        event.figures().stream()
                .sorted(Comparator.comparingInt(EpsFigure::fiscalYear))
                .forEach(figure -> text.append("\n- ")
                        .append(figure.fiscalYear())
                        .append(figure.estimated() ? "E" : "A")
                        .append(": ")
                        .append(EPS_FORMAT.format(figure.eps())));
        return text.toString();
    }
}
