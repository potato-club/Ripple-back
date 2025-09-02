package org.example.rippleback.core.error;


import java.util.Collections;
import java.util.Map;


public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> details;


    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }


    public BusinessException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.details = details != null ? details : Collections.emptyMap();
    }


    public ErrorCode errorCode() { return errorCode; }
    public Map<String, Object> details() { return details; }
}