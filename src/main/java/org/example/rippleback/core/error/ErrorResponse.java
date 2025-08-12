package org.example.rippleback.core.error;

public record ErrorResponse(int status, String code, String message) {
    public static ErrorResponse of(ErrorCode ec) {
        return new ErrorResponse(
                ec.httpStatus().value(),
                ec.code(),
                ec.message()
        );
    }
}
