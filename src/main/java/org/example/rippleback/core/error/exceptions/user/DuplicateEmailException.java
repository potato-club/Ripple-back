package org.example.rippleback.core.error.exceptions.user;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import java.util.Map;

public class DuplicateEmailException extends BusinessException {
    private final String email;

    public DuplicateEmailException(String email) {
        super(ErrorCode.DUPLICATE_EMAIL, Map.of("email", email));
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}