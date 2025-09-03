package org.example.rippleback.core.error.exceptions.comment;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class InvalidCommentThreadException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.INVALID_COMMENT_THREAD; }
}
