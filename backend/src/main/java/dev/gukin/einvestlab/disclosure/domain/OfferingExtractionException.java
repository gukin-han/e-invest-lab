package dev.gukin.einvestlab.disclosure.domain;

public class OfferingExtractionException extends RuntimeException {

    public OfferingExtractionException(String message) {
        super(message);
    }

    public OfferingExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
