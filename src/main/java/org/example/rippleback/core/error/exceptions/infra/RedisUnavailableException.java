package org.example.rippleback.core.error.exceptions.infra;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

import java.util.Map;

public class RedisUnavailableException extends BusinessException {
    public RedisUnavailableException() {
        super(ErrorCode.REDIS_UNAVAILABLE, Map.of());
    }
}
