package org.example.rippleback.core.error.exceptions.post;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;

public class CreditBalanceInconsistentException extends BusinessException {
    @Override public ErrorCode errorCode () { return ErrorCode.CREDIT_BALANCE_INCONSISTENT; }
}
