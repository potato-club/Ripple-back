package org.example.rippleback.core.error.exceptions.common;

public class ValidationErrorException extends RuntimeException {
    public ValidationErrorException(String message) {
        super(message);
    }
}
