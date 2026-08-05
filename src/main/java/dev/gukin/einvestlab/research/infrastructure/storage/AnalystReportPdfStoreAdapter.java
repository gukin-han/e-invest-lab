package dev.gukin.einvestlab.research.infrastructure.storage;

import dev.gukin.einvestlab.global.config.PdfStorageProperties;
import dev.gukin.einvestlab.research.domain.AnalystReportPdfStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AnalystReportPdfStoreAdapter implements AnalystReportPdfStore {

    private final PdfStorageProperties properties;

    @Override
    public String store(long reportIdx, LocalDate publishedDate, byte[] content) {
        String relativePath = "%d/%02d/%d.pdf".formatted(
                publishedDate.getYear(), publishedDate.getMonthValue(), reportIdx);
        Path target = Path.of(properties.root()).resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("PDF 저장 실패: " + target, e);
        }
        return relativePath;
    }
}
