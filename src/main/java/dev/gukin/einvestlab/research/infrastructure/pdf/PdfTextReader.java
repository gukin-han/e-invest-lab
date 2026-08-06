package dev.gukin.einvestlab.research.infrastructure.pdf;

import dev.gukin.einvestlab.research.domain.PdfTextExtractionException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class PdfTextReader {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    public PdfTextReader() {
        verifyBinaryPresent();
    }

    public String readLayoutText(Path pdfFile, int firstPages) {
        ProcessBuilder builder = new ProcessBuilder(
                "pdftotext", "-layout", "-f", "1", "-l", String.valueOf(firstPages),
                pdfFile.toString(), "-");
        try {
            Process process = builder.start();
            String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new PdfTextExtractionException("pdftotext 시간 초과: " + pdfFile);
            }
            if (process.exitValue() != 0) {
                throw new PdfTextExtractionException(
                        "pdftotext 종료 코드 " + process.exitValue() + ": " + pdfFile);
            }
            return text;
        } catch (IOException e) {
            throw new PdfTextExtractionException("pdftotext 실행 실패: " + pdfFile, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PdfTextExtractionException("pdftotext 대기 중단됨: " + pdfFile, e);
        }
    }

    private void verifyBinaryPresent() {
        try {
            Process process = new ProcessBuilder("pdftotext", "-v")
                    .redirectErrorStream(true)
                    .start();
            process.getInputStream().readAllBytes();
            process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "pdftotext 를 찾을 수 없음 — poppler 설치 필요 (macOS: brew install poppler)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("pdftotext 확인 중단됨", e);
        }
    }
}
