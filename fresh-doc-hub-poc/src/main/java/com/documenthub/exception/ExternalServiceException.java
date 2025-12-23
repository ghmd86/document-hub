package com.documenthub.exception;

/**
 * Exception thrown when an external service (ECMS, etc.) fails.
 */
public class ExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final int statusCode;

    public ExternalServiceException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = 0;
    }

    public ExternalServiceException(String serviceName, String message, int statusCode) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.statusCode = 0;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
