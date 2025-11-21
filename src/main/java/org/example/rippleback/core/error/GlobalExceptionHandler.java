package org.example.rippleback.core.error;


import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;

import static org.example.rippleback.core.error.ErrorCode.*;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode ec = e.errorCode();
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        var fieldErrors = e.getBindingResult().getFieldErrors();
        // S3ObjectKey 위반 있으면 INVALID_OBJECT_KEY로 특화
        boolean objectKeyViolation = fieldErrors.stream().anyMatch(fe -> fe.getCodes() != null && Arrays.stream(fe.getCodes()).anyMatch(code -> code.contains("S3ObjectKey")));
        if (objectKeyViolation) {
            ErrorCode ec = INVALID_OBJECT_KEY;
            return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
        }

        // 그 외
        ErrorCode ec = VALIDATION_ERROR;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
        String message = root.getMessage();

        // comment_report 유니크 제약 위반인지 확인
        if (message != null && message.contains("uq_comment_report_user_comment")) {
            ErrorCode ec = ErrorCode.ALREADY_REPORTED_COMMENT;
            return ResponseEntity.status(ec.httpStatus())
                    .body(ErrorResponse.of(ec));
        }

        // 그 외
        ErrorCode ec = ErrorCode.DB_ERROR;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied
            (AccessDeniedException e) {
        ErrorCode ec = FORBIDDEN;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleData
            (DataAccessException e) {
        ErrorCode ec = DB_ERROR;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        ErrorCode ec = INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ErrorResponse> handleMissing
            (Exception e) {
        ErrorCode ec = VALIDATION_ERROR;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }
}