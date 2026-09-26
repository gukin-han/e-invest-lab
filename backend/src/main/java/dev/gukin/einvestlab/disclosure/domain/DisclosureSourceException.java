package dev.gukin.einvestlab.disclosure.domain;

public class DisclosureSourceException extends RuntimeException {

    public DisclosureSourceException(String message) {
        super(message);
    }

    public DisclosureSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
