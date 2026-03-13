package com.example.integration.http.exception;

public class ThirdPartyRequestException extends RuntimeException {

    public ThirdPartyRequestException(String message) {
        super(message);
    }

    public ThirdPartyRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
