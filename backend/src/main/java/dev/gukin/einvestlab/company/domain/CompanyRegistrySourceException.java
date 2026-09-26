package dev.gukin.einvestlab.company.domain;

public class CompanyRegistrySourceException extends RuntimeException {

    public CompanyRegistrySourceException(String message) {
        super(message);
    }

    public CompanyRegistrySourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
