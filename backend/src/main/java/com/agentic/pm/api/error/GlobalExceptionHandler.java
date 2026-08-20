package com.agentic.pm.api.error;

import com.agentic.pm.exception.InvalidFileException;
import com.agentic.pm.exception.ProjectNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, InvalidFileException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        log.warn("Bad request: {}", e.getMessage(), e);
        return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ProjectNotFoundException e) {
        log.info("Not found: {}", e.getMessage());
        return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleServerError(Exception e, HttpServletRequest request) {
        log.error("Unhandled server error on {} {}",
                request.getMethod(),
                request.getRequestURI(),
                e);
        return ResponseEntity.status(500).body(Map.of("message", "Internal server error"));
    }
}

