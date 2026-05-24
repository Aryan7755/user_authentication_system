package com.aryan.project7.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

// This service is our specialized "cookie baker."
// It handles creating, sending, and destroying cookies for the user session.
@Service
@Getter
public class CookieService {

    private final Logger logger = LoggerFactory.getLogger(CookieService.class);

    private final String refreshTokenCookieName;
    private final boolean cookieHttpOnly;
    private final boolean cookieSecure;
    private final String cookieDomain;
    private final String cookieSameSite;

    // We pull all our cookie settings from application.properties so we can
    // easily change them between local dev and production.
    public CookieService(
            @Value("${security.jwt.refresh-token-cookie-name}") String refreshTokenCookieName,
            @Value("${security.jwt.cookie-http-only}") boolean cookieHttpOnly,
            @Value("${security.jwt.cookie-secure}") boolean cookieSecure,
            @Value("${security.jwt.cookie-domain}") String cookieDomain,
            @Value("${security.jwt.cookie-same-site}") String cookieSameSite) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.cookieHttpOnly = cookieHttpOnly;
        this.cookieSecure = cookieSecure;
        this.cookieDomain = cookieDomain;
        this.cookieSameSite = cookieSameSite;
    }

    // This method takes a refresh token and "staples" it to the response as a cookie
    public void attachRefreshCookie(HttpServletResponse response, String value, int maxAge) {

        logger.info("Attaching cookie with name: {} and value: {} ", refreshTokenCookieName, value);

        var responseCookieBuilder = ResponseCookie.from(refreshTokenCookieName, value)
                // HttpOnly means JavaScript can't touch this—huge for security!
                .httpOnly(cookieHttpOnly)
                // Secure means it only travels over HTTPS
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                // SameSite helps prevent CSRF attacks
                .sameSite(cookieSameSite);

        // If we have a specific domain (like .ourapp.com), we set it here
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            responseCookieBuilder.domain(cookieDomain);
        }

        ResponseCookie responseCookie = responseCookieBuilder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }

    // When someone logs out, we send an empty cookie with a maxAge of 0 to kill the old one
    public void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, "")
                .maxAge(0)
                .httpOnly(cookieHttpOnly)
                .path("/")
                .sameSite(cookieSameSite)
                .secure(cookieSecure);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        ResponseCookie responseCookie = builder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }

    // This tells the browser: "Don't store this sensitive info in your local cache!"
    public void addNoStoreHeader(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Pragma", "no-cache");
    }
    public String getRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (refreshTokenCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}