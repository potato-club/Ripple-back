package org.example.rippleback.core.error.exceptions.post;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class AlreadyBookmarkedException extends BusinessException {
    @Override public ErrorCode errorCode() { return  ErrorCode.ALREADY_BOOKMARKED; }
}
