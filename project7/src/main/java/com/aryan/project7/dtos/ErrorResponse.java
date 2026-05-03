package com.aryan.project7.dtos;

import org.springframework.http.HttpStatus;

public record ErrorResponse(
    String message,
    HttpStatus status
){}
