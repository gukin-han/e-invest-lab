package dev.gukin.einvestlab.research.domain;

import java.nio.file.Path;

public interface EpsExtractor {

    EpsExtraction extract(Path pdfFile);
}
