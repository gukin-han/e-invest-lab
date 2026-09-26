package dev.gukin.einvestlab.market.domain;

public class MarketSourceException extends RuntimeException {

    public MarketSourceException(String message) {
        super(message);
    }

    public MarketSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
