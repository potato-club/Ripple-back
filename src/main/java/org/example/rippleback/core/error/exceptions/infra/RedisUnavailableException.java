package org.example.rippleback.core.error.exceptions.infra;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class RedisUnavailableException extends BusinessException {
    @Override public ErrorCode errorCode() { return ErrorCode.REDIS_UNAVAILABLE; }
}
