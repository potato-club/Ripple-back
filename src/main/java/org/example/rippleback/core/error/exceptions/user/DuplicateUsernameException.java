package org.example.rippleback.core.error.exceptions.user;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class DuplicateUsernameException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.DUPLICATE_USERNAME; }
}
