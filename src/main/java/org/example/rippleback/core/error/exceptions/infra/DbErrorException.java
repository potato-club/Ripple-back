package org.example.rippleback.core.error.exceptions.infra;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

import java.util.Map;

public class InfraDbErrorException extends BusinessException {
    public InfraDbErrorException(String query, Throwable cause) {
        super(ErrorCode.DB_ERROR, Map.of("query", query, "cause", cause == null ? "null" : cause.getMessage()));
    }
}