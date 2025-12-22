package com.documenthub.integration.ecms;

import com.documenthub.integration.ecms.dto.EcmsErrorResponse;
import lombok.Getter;

/**
 * Exception thrown when ECMS API call fails
 */
@Getter
public class EcmsClientException extends RuntimeException {

    private final int statusCode;
    private final EcmsErrorResponse errorResponse;

    public EcmsClientException(String message) {
        super(message);
        this.statusCode = 500;
        this.errorResponse = null;
    }

    public EcmsClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
        this.errorResponse = null;
    }

    public EcmsClientException(int statusCode, EcmsErrorResponse errorResponse) {
        super(errorResponse != null ? errorResponse.getMessage() : "ECMS API error");
        this.statusCode = statusCode;
        this.errorResponse = errorResponse;
    }

    public EcmsClientException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorResponse = null;
    }
}
