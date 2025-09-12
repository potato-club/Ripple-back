package org.example.rippleback.core.error.exceptions.media;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class InvalidObjectKeyException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.INVALID_OBJECT_KEY; }
}
