package com.aryan.project7.security;

import com.aryan.project7.repository.RefreshTokenRedisRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RefreshTokenFilter extends OncePerRequestFilter {

    private final RefreshTokenRedisRepo redisRepo;
    private final CookieService cookieService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only trigger on your refresh endpoint
        if (!request.getRequestURI().equals("/api/v1/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = cookieService.getRefreshCookie(request);
        if (refreshToken == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String hashedToken = DigestUtils.sha256Hex(refreshToken);

        // --- THE SECURITY CORE ---
        if (redisRepo.existsById(hashedToken)) {
            // Token is valid!
            // Delete it immediately (Single-use rotation pattern)
            redisRepo.deleteById(hashedToken);
            filterChain.doFilter(request, response);
        } else {
            // Token either doesn't exist or was already used (Replay attack detected!)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
