package org.example.rippleback.core.error.exceptions.auth;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class SessionInvalidatedException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.SESSION_INVALIDATED; }
}