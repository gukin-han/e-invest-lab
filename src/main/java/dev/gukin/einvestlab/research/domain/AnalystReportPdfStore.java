package dev.gukin.einvestlab.research.domain;

import java.time.LocalDate;

public interface AnalystReportPdfStore {

    String store(long reportIdx, LocalDate publishedDate, byte[] content);
}
