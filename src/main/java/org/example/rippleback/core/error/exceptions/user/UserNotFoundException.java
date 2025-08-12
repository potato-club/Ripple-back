package org.example.rippleback.core.error.exceptions.user;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class UserNotFoundException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.USER_NOT_FOUND; }
}