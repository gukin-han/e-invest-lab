package dev.gukin.einvestlab.research.domain;

public class EpsNotificationException extends RuntimeException {

    public EpsNotificationException(String message) {
        super(message);
    }

    public EpsNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
