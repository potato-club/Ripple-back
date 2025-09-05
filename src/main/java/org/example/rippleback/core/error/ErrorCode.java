package org.example.rippleback.core.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // COMMON (9000~9099)
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "9000", "요청 형식이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "9001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "9002", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "9003", "요청한 자원을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "9009", "서버 내부 오류가 발생했습니다."),

    // USER (1000~1099)
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "1000", "중복된 ID입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "1001", "중복된 이메일입니다."),
    EMAIL_CODE_INVALID(HttpStatus.BAD_REQUEST, "1002", "인증번호가 올바르지 않습니다."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "1003", "인증번호가 만료되었습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "1004", "이메일 인증이 완료되지 않았습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "1005", "사용자를 찾을 수 없습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "1006", "비활성화된 사용자입니다."),

    FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "1011", "이미 팔로우한 상태입니다."),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "1012", "팔로우 상태가 아닙니다."),
    BLOCK_ALREADY_EXISTS(HttpStatus.CONFLICT, "1013", "이미 차단한 상태입니다."),
    BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "1014", "차단 상태가 아닙니다."),
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "1015", "자기 자신을 팔로우할 수 없습니다."),
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "1016", "자기 자신을 차단할 수 없습니다."),
    FOLLOW_NOT_ALLOWED_YOU_BLOCKED_TARGET(HttpStatus.FORBIDDEN, "1017", "차단한 사용자는 팔로우할 수 없습니다."),

    // AUTH (1100~1199)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "1100", "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "1101", "인증 토큰이 없습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "1102", "유효하지 않은 인증 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "1103", "인증 토큰이 만료되었습니다."),
    TOKEN_TYPE_INVALID(HttpStatus.UNAUTHORIZED, "1104", "토큰 타입이 올바르지 않습니다."),
    DEVICE_MISMATCH(HttpStatus.UNAUTHORIZED, "1105", "디바이스 정보가 일치하지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "1106", "리프레시 토큰이 존재하지 않습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "1107", "리프레시 토큰 정보가 일치하지 않습니다."),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "1108", "리프레시 토큰 재사용이 감지되었습니다."),
    SESSION_INVALIDATED(HttpStatus.UNAUTHORIZED, "1109", "세션이 무효화되었습니다."),

    // IMAGE/MEDIA (1600~1699)
    INVALID_IMAGE_EXTENSION(HttpStatus.BAD_REQUEST, "1600", "허용되지 않은 확장자입니다."),
    INVALID_IMAGE_MIME_TYPE(HttpStatus.BAD_REQUEST, "1601", "허용되지 않은 MIME 타입입니다."),
    CORRUPTED_IMAGE_DATA(HttpStatus.BAD_REQUEST, "1602", "손상된 이미지 데이터입니다."),
    INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "1603", "유효하지 않은 이미지 URL입니다."),

    // INFRA (2000~2099)
    DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "2000", "데이터베이스 오류가 발생했습니다."),
    REDIS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "2001", "Redis 연결에 문제가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}