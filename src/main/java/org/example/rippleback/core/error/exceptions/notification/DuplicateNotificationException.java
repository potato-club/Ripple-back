package org.example.rippleback.core.error.exceptions.notification;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class DuplicateNotificationException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.DUPLICATE_NOTIFICATION; }
}
