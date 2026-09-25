package com.careerpulse.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException e
    ) {

        String code;

        if (e.getStatusCode().value() == 409) {
            code = "USER_ALREADY_EXISTS";
        } else if (e.getStatusCode().value() == 400) {
            code = "INVALID_REQUEST";
        } else {
            code = "REQUEST_FAILED";
        }

        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of(
                        "code", code,
                        "message",
                        e.getReason() != null
                                ? e.getReason()
                                : "요청 처리에 실패했습니다."
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException e
    ) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .orElse("입력값을 확인해 주세요.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "code", "VALIDATION_ERROR",
                        "message", message
                ));
    }
}