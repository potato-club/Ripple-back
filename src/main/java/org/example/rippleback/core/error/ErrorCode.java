package org.example.rippleback.core.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // COMMON (9000~9099)
    COMMON_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "9000", "요청 형식이 올바르지 않습니다."),
    COMMON_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "9001", "인증이 필요합니다."),
    COMMON_FORBIDDEN(HttpStatus.FORBIDDEN, "9002", "접근 권한이 없습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "9003", "요청한 자원을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "9009", "서버 내부 오류가 발생했습니다."),

    // USER (1000~1099)
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "1000", "중복된 ID입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "1001", "중복된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "1005", "사용자를 찾을 수 없습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "1006", "비활성화된 사용자입니다."),

    // AUTH (1100~1199)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "1100", "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "1101", "인증 토큰이 없습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "1102", "유효하지 않은 인증 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "1103", "인증 토큰이 만료되었습니다."),
    TOKEN_TYPE_INVALID(HttpStatus.UNAUTHORIZED, "1104", "토큰 타입이 올바르지 않습니다."),
    DEVICE_MISMATCH(HttpStatus.UNAUTHORIZED, "1105", "디바이스 정보가 일치하지 않습니다."),
    REFRESH_NOT_FOUND(HttpStatus.UNAUTHORIZED, "1106", "리프레시 토큰이 존재하지 않습니다."),
    REFRESH_MISMATCH(HttpStatus.UNAUTHORIZED, "1107", "리프레시 토큰 정보가 일치하지 않습니다."),
    REFRESH_REUSED(HttpStatus.UNAUTHORIZED, "1108", "리프레시 토큰 재사용이 감지되었습니다."),
    SESSION_INVALIDATED(HttpStatus.UNAUTHORIZED, "1109", "세션이 무효화되었습니다."),

    // INFRA (2000~2099)
    INFRA_DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "2000", "데이터베이스 오류가 발생했습니다."),
    INFRA_REDIS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "2001", "Redis 연결에 문제가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus httpStatus() { return httpStatus; }
    public String code() { return code; }
    public String message() { return message; }
}
