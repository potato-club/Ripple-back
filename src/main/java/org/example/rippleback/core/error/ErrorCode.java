package org.example.rippleback.core.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // COMMON (9000–9099)
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "9000", "요청 형식이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "9001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "9002", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "9003", "요청한 자원을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "9004", "서버 내부 오류가 발생했습니다."),

    // USER (1000–1099)
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "1000", "중복된 ID입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "1001", "중복된 이메일입니다."),
    EMAIL_CODE_INVALID(HttpStatus.BAD_REQUEST, "1002", "인증번호가 올바르지 않습니다."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "1003", "인증번호가 만료되었습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "1004", "이메일 인증이 완료되지 않았습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "1005", "사용자를 찾을 수 없습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "1006", "비활성화된 사용자입니다."),
    FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "1007", "이미 팔로우한 상태입니다."),
    BLOCK_ALREADY_EXISTS(HttpStatus.CONFLICT, "1008", "이미 차단한 상태입니다."),
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "1009", "자기 자신을 팔로우할 수 없습니다."),
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "1010", "자기 자신을 차단할 수 없습니다."),
    FOLLOW_NOT_ALLOWED_YOU_BLOCKED_TARGET(HttpStatus.FORBIDDEN, "1011", "차단한 사용자는 팔로우할 수 없습니다."),

    // AUTH (1100–1199)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "1100", "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "1101", "인증 토큰이 없습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "1102", "유효하지 않은 인증 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "1103", "인증 토큰이 만료되었습니다."),
    TOKEN_TYPE_INVALID(HttpStatus.UNAUTHORIZED, "1104", "토큰 타입이 올바르지 않습니다."),
    DEVICE_MISMATCH(HttpStatus.UNAUTHORIZED, "1105", "디바이스 정보가 일치하지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "1106", "RT가 존재하지 않습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "1107", "RT 정보가 일치하지 않습니다."),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "1108", "RT 재사용이 감지되었습니다."),
    SESSION_INVALIDATED(HttpStatus.UNAUTHORIZED, "1109", "세션이 무효화되었습니다."),

    // FEED (1200–1299)
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "1200", "포스트를 찾을 수 없습니다."),
    INVALID_IMAGE_COUNT(HttpStatus.BAD_REQUEST, "1201", "이미지 입력 형식이 올바르지 않습니다."), // 개수/순서/중복 포괄
    INVALID_VIDEO_COUNT(HttpStatus.BAD_REQUEST, "1202", "포스트당 비디오는 최대 1개까지 허용됩니다."),
    INVALID_VIDEO_DURATION(HttpStatus.BAD_REQUEST, "1203", "비디오 길이는 3–180초 사이여야 합니다."),
    ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "1204", "이미 북마크된 상태입니다."),
    ALREADY_LIKED_FEED(HttpStatus.CONFLICT, "1205", "이미 좋아요를 누른 상태입니다."),
    INSUFFICIENT_CREDITS(HttpStatus.BAD_REQUEST, "1206", "잔여 보기 크레딧이 부족합니다."),
    CREDIT_BALANCE_INCONSISTENT(HttpStatus.INTERNAL_SERVER_ERROR, "1207", "피드 크레딧 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),

    // COMMENT (1300–1399)
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "1300", "댓글을 찾을 수 없습니다."),
    INVALID_COMMENT_THREAD(HttpStatus.BAD_REQUEST, "1301", "잘못된 댓글 스레드 구조입니다."),
    COMMENT_DELETE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "1302", "댓글을 삭제할 권한이 없습니다."),
    COMMENT_CONTENT_INVALID(HttpStatus.BAD_REQUEST, "1303", "댓글 내용이 올바르지 않습니다."),
    ALREADY_LIKED_COMMENT(HttpStatus.CONFLICT, "1304", "이미 좋아요를 누른 상태입니다."),
    ALREADY_REPORTED_COMMENT(HttpStatus.CONFLICT, "1305", "이미 접수된 신고가 있습니다."),
    COMMENT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "1306", "notes가 비어 있을 수 없습니다."),

    // NOTIFICATION (1400–1499)
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "1400", "알림을 찾을 수 없습니다."),
    DUPLICATE_NOTIFICATION(HttpStatus.CONFLICT, "1401", "중복된 알림입니다."),

    // MEDIA (1600–1699)
    INVALID_MEDIA_TYPE(HttpStatus.BAD_REQUEST, "1600", "허용되지 않은 미디어 형식입니다."),
    INVALID_OBJECT_KEY(HttpStatus.BAD_REQUEST, "1601", "유효하지 않은 object key입니다."),

    // INFRA (2000–2099)
    DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "2000", "데이터베이스 오류가 발생했습니다."),
    REDIS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "2001", "Redis 연결에 문제가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
