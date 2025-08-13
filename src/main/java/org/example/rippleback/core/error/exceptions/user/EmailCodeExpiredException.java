package org.example.rippleback.core.error.exceptions.user;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class EmailCodeExpiredException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.EMAIL_CODE_EXPIRED; }
}
