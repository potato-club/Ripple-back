package org.example.rippleback.core.error.exceptions.auth;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class TokenMissingException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.TOKEN_MISSING; }
}