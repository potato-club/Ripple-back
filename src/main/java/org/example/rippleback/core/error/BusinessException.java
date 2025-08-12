package org.example.rippleback.core.error;

public abstract class BusinessException extends RuntimeException {
    public BusinessException() { super(); }
    public BusinessException(Throwable cause) { super(cause); }
    public abstract ErrorCode errorCode();
}