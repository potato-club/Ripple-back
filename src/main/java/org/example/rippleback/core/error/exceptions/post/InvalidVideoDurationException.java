package org.example.rippleback.core.error.exceptions.post;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class InvalidVideoDurationException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.INVALID_VIDEO_DURATION; }
}
