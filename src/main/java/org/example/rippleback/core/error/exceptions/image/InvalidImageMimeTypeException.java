package org.example.rippleback.core.error.exceptions.image;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class InvalidImageMimeTypeException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.INVALID_IMAGE_MIME_TYPE; }
}
