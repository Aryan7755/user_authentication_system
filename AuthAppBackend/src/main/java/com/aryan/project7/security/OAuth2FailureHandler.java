package com.aryan.project7.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${app.auth.frontend.failure-redirect}")
    private String frontendFailureUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String errorMessage = URLEncoder.encode(
            exception.getMessage() != null ? exception.getMessage() : "Authentication failed",
            StandardCharsets.UTF_8
        );

        response.sendRedirect(frontendFailureUrl + "?error=" + errorMessage);
    }
}