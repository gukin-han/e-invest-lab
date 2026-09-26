package dev.gukin.einvestlab.research.domain;

public class PdfTextExtractionException extends RuntimeException {

    public PdfTextExtractionException(String message) {
        super(message);
    }

    public PdfTextExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
