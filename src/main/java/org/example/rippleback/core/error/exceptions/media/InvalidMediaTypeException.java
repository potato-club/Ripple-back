package org.example.rippleback.core.error.exceptions.media;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class InvalidMediaTypeException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.INVALID_MEDIA_TYPE; }
}
