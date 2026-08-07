package dev.gukin.einvestlab.research.domain;

import java.nio.file.Path;
import java.time.LocalDate;

public interface AnalystReportPdfStore {

    String store(long reportIdx, LocalDate publishedDate, byte[] content);

    Path resolve(String relativePath);

    boolean exists(String relativePath);
}
