package org.example.rippleback.core.error.exceptions.notification;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class NotificationNotFoundException extends BusinessException {
    @Override public ErrorCode errorCode() { return  ErrorCode.NOTIFICATION_NOT_FOUND; }
}
