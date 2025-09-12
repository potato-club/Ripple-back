package org.example.rippleback.core.error.exceptions.comment;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class CommentNotFoundException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.COMMENT_NOT_FOUND; }
}
