package com.gemfire.demo.exception;

import com.gemfire.demo.model.CommonResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Global exception handler.
 *
 * <p>Centralizes error handling (SRP, DRY) so controllers stay clean.
 * Maps exceptions to appropriate HTTP status codes and uniform error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponseDTO<Void>> handleValidation(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(CommonResponseDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<CommonResponseDTO<Void>> handleNotFound(NoSuchElementException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CommonResponseDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponseDTO<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponseDTO.error("Internal server error: " + ex.getMessage()));
    }
}
