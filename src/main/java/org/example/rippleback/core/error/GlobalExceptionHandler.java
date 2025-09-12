package org.example.rippleback.core.error;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestHeaderException;

import static org.example.rippleback.core.error.ErrorCode.*;
import java.util.Arrays;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        var ec = e.errorCode();
        return ResponseEntity.status(ec.httpStatus())
                .body(ErrorResponse.of(ec));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        var fieldErrors = e.getBindingResult().getFieldErrors();

        // 1) S3ObjectKey 위반 있으면 INVALID_OBJECT_KEY로 특화
        boolean objectKeyViolation = fieldErrors.stream().anyMatch(fe ->
                fe.getCodes() != null && Arrays.stream(fe.getCodes()).anyMatch(code -> code.contains("S3ObjectKey"))
        );
        if (objectKeyViolation) {
            var ec = INVALID_OBJECT_KEY; // 1601
            return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
        }

        // 2) 그 외는 공통 9000
        var ec = VALIDATION_ERROR;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        var ec = FORBIDDEN;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleData(DataAccessException e) {
        var ec = DB_ERROR;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        var ec = INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }

    @ExceptionHandler({ MissingServletRequestParameterException.class, MissingRequestHeaderException.class })
    public ResponseEntity<ErrorResponse> handleMissing(Exception e) {
        var ec = VALIDATION_ERROR;
        return ResponseEntity.status(ec.httpStatus()).body(ErrorResponse.of(ec));
    }
}
