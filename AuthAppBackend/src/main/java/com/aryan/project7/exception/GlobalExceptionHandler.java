package com.aryan.project7.exception;

import com.aryan.project7.dtos.ApiError;
import com.aryan.project7.dtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// This class listens across the whole app and catches any errors before they reach the user
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Unifies all Auth exceptions into the ApiError format
    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class,
            CredentialsExpiredException.class,
            DisabledException.class
    })
    public ResponseEntity<ApiError> handleAuthException(Exception e, HttpServletRequest request) {
        logger.info("Auth Exception: {}", e.getMessage());
        return ResponseEntity.badRequest().body(
                ApiError.of(HttpStatus.BAD_REQUEST.value(), "Authentication Issue", e.getMessage(), request.getRequestURI(), false)
        );
    }

    // Unifies ResourceNotFound
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), request.getRequestURI(), false)
        );
    }

    // Unifies Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String msg = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(
                ApiError.of(HttpStatus.BAD_REQUEST.value(), "Validation Failed", msg, request.getRequestURI(), false)
        );
    }

    // Unifies General/Illegal Arguments
    @ExceptionHandler({IllegalArgumentException.class, Exception.class})
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest request) {
        if (!(ex instanceof IllegalArgumentException)) {
            logger.error("Internal Server Error: ", ex); // Log the full stack trace for 500 errors
        }

        HttpStatus status = (ex instanceof IllegalArgumentException) ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        String title = (ex instanceof IllegalArgumentException) ? "Invalid Request" : "Internal Server Error";

        return ResponseEntity.status(status).body(
                ApiError.of(status.value(), title, ex.getMessage(), request.getRequestURI(), false)
        );
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> handleRateLimit(RateLimitException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                ApiError.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Too Many Requests",
                        ex.getMessage(), // This will now resolve correctly!
                        request.getRequestURI(),
                        false
                )
        );
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiError.of(HttpStatus.FORBIDDEN.value(), "Forbidden", "You do not have permission to access this resource", request.getRequestURI(), false)
        );
    }
}