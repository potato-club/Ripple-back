package org.example.rippleback.core.error.exceptions.auth;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class TokenTypeInvalidException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.TOKEN_TYPE_INVALID; }
}