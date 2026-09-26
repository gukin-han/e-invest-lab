package dev.gukin.einvestlab.research.domain;

public interface AnalystReportPdfSource {

    byte[] fetchPdf(long reportIdx);
}
