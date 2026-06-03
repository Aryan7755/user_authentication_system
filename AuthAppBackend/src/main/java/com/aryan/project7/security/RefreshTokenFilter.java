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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RefreshTokenFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(RefreshTokenFilter.class);

    private final RefreshTokenRedisRepo redisRepo;
    private final CookieService cookieService;
    private final JwtService jwtService; // Ensure this is injected
    private final CustomUserDetailService customUserDetailService; // Ensure this is injected

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        logger.info("FILTER CHECK: Request URI: {}", request.getRequestURI());

        String refreshToken = cookieService.getRefreshTokenFromCookie(request);

        if (refreshToken == null || refreshToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String hashedToken = DigestUtils.sha256Hex(refreshToken);
        boolean exists = redisRepo.existsById(hashedToken);

        if (!exists) {
            logger.warn("REJECTED: Token hash {} not found in Redis!", hashedToken);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // --- AUTHENTICATION LOGIC ---
        // Only set the context if it's not already set
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String userEmail = jwtService.extractEmail(refreshToken); // Extract email from JWT

            if (userEmail != null) {
                var userDetails = customUserDetailService.loadUserByUsername(userEmail);
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.info("Successfully authenticated user: {}", userEmail);
            }
        }

        filterChain.doFilter(request, response);
    }
}