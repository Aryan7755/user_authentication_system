package com.aryan.project7.security;

import com.aryan.project7.repository.RefreshTokenRedisRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RefreshTokenFilter extends OncePerRequestFilter {

    // Added the missing logger definition
    private final Logger logger = LoggerFactory.getLogger(RefreshTokenFilter.class);

    private final RefreshTokenRedisRepo redisRepo;
    private final CookieService cookieService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Only trigger if the user is hitting the refresh endpoint
        if (!request.getRequestURI().contains("/api/v1/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Get the token
        String refreshToken = cookieService.getRefreshCookie(request);

        // 3. Perform a null check
        if (refreshToken == null || refreshToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Verify token existence in Redis
        String hashedToken = DigestUtils.sha256Hex(refreshToken);
        boolean exists = redisRepo.existsById(hashedToken);

        // 5. Debug logging
        logger.info("FILTER DEBUG -> Cookie Value: {}", refreshToken.substring(0, Math.min(refreshToken.length(), 10)) + "...");
        logger.info("FILTER DEBUG -> Hashed Token: {}", hashedToken);
        logger.info("FILTER DEBUG -> Exists in Redis: {}", exists);

        if (!exists) {
            logger.warn("REJECTED: Token hash {} not found in Redis!", hashedToken);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return; // Stop the chain here
        }

        // Token exists, proceed
        filterChain.doFilter(request, response);
    }
}