package dev.gukin.einvestlab.research.infrastructure.hankyung;

import dev.gukin.einvestlab.global.config.HankyungApiProperties;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfSource;
import dev.gukin.einvestlab.research.domain.ResearchSourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AnalystReportPdfSourceAdapter implements AnalystReportPdfSource {

    private static final Duration DOWNLOAD_DELAY = Duration.ofSeconds(1);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};

    private final HttpClient httpClient;
    private final HankyungApiProperties properties;

    @Override
    public byte[] fetchPdf(long reportIdx) {
        delay();
        HttpRequest request = HttpRequest.newBuilder(buildPdfUri(reportIdx))
                .header("User-Agent", "Mozilla/5.0")
                .timeout(RESPONSE_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new ResearchSourceException(
                        "한경 컨센서스 PDF HTTP " + response.statusCode() + " (report_idx=" + reportIdx + ")");
            }
            return requirePdf(response.body(), reportIdx);
        } catch (IOException e) {
            throw new ResearchSourceException("한경 컨센서스 PDF 요청 실패 (report_idx=" + reportIdx + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResearchSourceException("한경 컨센서스 PDF 요청 중단됨", e);
        }
    }

    URI buildPdfUri(long reportIdx) {
        return URI.create(properties.baseUrl() + "/analysis/downpdf?report_idx=" + reportIdx);
    }

    static byte[] requirePdf(byte[] body, long reportIdx) {
        if (body.length < PDF_MAGIC.length
                || !Arrays.equals(body, 0, PDF_MAGIC.length, PDF_MAGIC, 0, PDF_MAGIC.length)) {
            throw new ResearchSourceException(
                    "PDF 아닌 응답 본문 (report_idx=" + reportIdx + ", 원천 에러 페이지 가능성)");
        }
        return body;
    }

    private void delay() {
        try {
            Thread.sleep(DOWNLOAD_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResearchSourceException("다운로드 간 대기 중단됨", e);
        }
    }
}
