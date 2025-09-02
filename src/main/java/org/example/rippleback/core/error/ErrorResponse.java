package org.example.rippleback.core.error;


import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;


public class ErrorResponse {
    private final String code;
    private final String message;


    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Map<String, Object> details;


    public ErrorResponse(String code, String message, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.details = details == null ? Map.of() : details;
    }


    public String getCode() { return code; }
    public String getMessage() { return message; }
    public Map<String, Object> getDetails() { return details; }


    public static ErrorResponse from(BusinessException e) {
        return new ErrorResponse(e.errorCode().code(), e.errorCode().message(), e.details());
    }
}