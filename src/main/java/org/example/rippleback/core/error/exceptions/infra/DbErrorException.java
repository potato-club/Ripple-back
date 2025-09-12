package org.example.rippleback.core.error.exceptions.infra;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class DbErrorException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.DB_ERROR; }
}
