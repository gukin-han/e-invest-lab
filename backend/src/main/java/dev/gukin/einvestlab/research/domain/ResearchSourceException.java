package dev.gukin.einvestlab.research.domain;

public class ResearchSourceException extends RuntimeException {

    public ResearchSourceException(String message) {
        super(message);
    }

    public ResearchSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
