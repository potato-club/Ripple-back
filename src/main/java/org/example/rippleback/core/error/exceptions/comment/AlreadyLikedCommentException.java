package org.example.rippleback.core.error.exceptions.comment;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class AlreadyLikedCommentException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.ALREADY_LIKED_COMMENT; }
}
