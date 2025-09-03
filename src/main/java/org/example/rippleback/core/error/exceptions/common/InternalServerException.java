package org.example.rippleback.core.error.exceptions.common;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class InternalServerException extends BusinessException {
    @Override
    public ErrorCode errorCode() {
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }
}
