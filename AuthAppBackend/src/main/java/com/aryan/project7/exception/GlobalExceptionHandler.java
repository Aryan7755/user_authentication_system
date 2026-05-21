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

    // This block handles all the common "Authentication" headaches like wrong passwords or disabled accounts
    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class,
            CredentialsExpiredException.class,
            DisabledException.class
    })
    public ResponseEntity<ApiError> handleAuthException(Exception e, HttpServletRequest servletRequest){
        // We log what happened so we can track issues in the console
        logger.info("Auth Exception caught: {} ", e.getClass().getName());

        // We use our ApiError DTO here to keep the error format consistent
        var apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Authentication Issue",
                e.getMessage(),
                servletRequest.getRequestURI()
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    // Custom handler for when we look for something in the DB (like a User) and it's just not there
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception){
        ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), HttpStatus.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Standard handler for when a method receives a bad argument (like an empty string where it shouldn't be)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception){
        ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Error");
        response.put("message", "Invalid request parameters");
        response.put("path", request.getRequestURI());
        response.put("fieldErrors", errors);

        return ResponseEntity.badRequest().body(response);
    }
}