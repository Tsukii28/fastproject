package com.example.fastproject.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                ex.getReason() != null ? ex.getReason() : "Lỗi xử lý yêu cầu",
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Giá trị không hợp lệ",
                        (existing, replacement) -> existing));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu đầu vào không hợp lệ",
                request.getRequestURI(),
                fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleMalformedJson(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Định dạng JSON không hợp lệ",
                request.getRequestURI(),
                null);
    }

    private ResponseEntity<?> buildErrorResponse(HttpStatus status,
            String message,
            String path,
            Map<String, String> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}