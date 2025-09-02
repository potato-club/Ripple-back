package org.example.rippleback.core.error.exceptions.image;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class CorruptedImageDataException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.CORRUPTED_IMAGE_DATA; }
}
