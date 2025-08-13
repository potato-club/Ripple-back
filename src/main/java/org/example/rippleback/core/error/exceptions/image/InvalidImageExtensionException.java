package org.example.rippleback.core.error.exceptions.image;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class InvalidImageExtensionException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.INVALID_IMAGE_EXTENSION; }
}
