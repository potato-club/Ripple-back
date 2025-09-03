package org.example.rippleback.core.error.exceptions.common;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class NotFoundException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.NOT_FOUND; }
}
