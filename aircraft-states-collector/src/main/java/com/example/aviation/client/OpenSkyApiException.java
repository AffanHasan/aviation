package com.example.aviation.client;

public class OpenSkyApiException extends RuntimeException {

    private final int statusCode;
    private final Long retryAfterSeconds;

    public OpenSkyApiException(String message, int statusCode, Long retryAfterSeconds) {
        super(message);
        this.statusCode = statusCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public OpenSkyApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.retryAfterSeconds = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
