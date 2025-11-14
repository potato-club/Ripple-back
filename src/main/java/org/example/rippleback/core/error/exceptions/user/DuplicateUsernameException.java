package org.example.rippleback.core.error.exceptions.user;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import java.util.Map;

public class DuplicateUsernameException extends BusinessException {
    private final String username;

    public DuplicateUsernameException(String username) {
        super(ErrorCode.DUPLICATE_USERNAME, Map.of("username", username));
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}