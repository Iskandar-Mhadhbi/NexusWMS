package com.nexuswms.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException; 
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice; 
import com.nexuswms.common.dto.ApiErrorResponse;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(404, ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(409, ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ApiErrorResponse(422, ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage() != null
                    ? error.getDefaultMessage()
                    : "Invalid value for field: " + error.getField())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(400, "Validation failed", errors, LocalDateTime.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(403, "Access denied", LocalDateTime.now()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(401, ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(500, "Internal server error", LocalDateTime.now()));
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity.status(400)
                .body(new ApiErrorResponse(400, "Malformed JSON request body", LocalDateTime.now()));
    }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405)
                .body(new ApiErrorResponse(405, "Method not allowed: " + ex.getMethod(), LocalDateTime.now()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(400)
                .body(new ApiErrorResponse(400, ex.getMessage(), LocalDateTime.now()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
            409, "Data integrity violation: a referenced resource does not exist or a unique constraint was violated",
            null, LocalDateTime.now()));
    }        
}          