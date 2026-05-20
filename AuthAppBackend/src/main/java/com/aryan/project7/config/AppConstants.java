package com.aryan.project7.config;

public class AppConstants {

    // These are the paths we want everyone to access without needing a login
    public static final String[] AUTH_PUBLIC_URLS = {

            // This is where the login/signup magic happens
            "/api/v1/auth/**",

            // Swagger and API docs—super helpful for testing so we keep them open
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };
}