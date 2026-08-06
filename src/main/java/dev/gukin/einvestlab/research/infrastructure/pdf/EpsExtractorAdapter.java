package dev.gukin.einvestlab.research.infrastructure.pdf;

import dev.gukin.einvestlab.research.domain.EpsExtraction;
import dev.gukin.einvestlab.research.domain.EpsExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class EpsExtractorAdapter implements EpsExtractor {

    private static final int SUMMARY_TABLE_PAGES = 2;

    private final PdfTextReader textReader;
    private final EpsSummaryTableParser parser;

    @Override
    public EpsExtraction extract(Path pdfFile) {
        return parser.parse(textReader.readLayoutText(pdfFile, SUMMARY_TABLE_PAGES));
    }
}
