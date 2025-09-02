package org.example.rippleback.core.error.exceptions.auth;


import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;


public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}