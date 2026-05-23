package com.aryan.project7.security;

import com.aryan.project7.helper.UserHelper;
import com.aryan.project7.repository.UserRepository;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            // Safety check: ensure it's an access token
            if (!jwtService.isAccessToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Parse token
            Jws<Claims> jwsClaims = jwtService.parse(token);
            Claims payload = jwsClaims.getPayload();
            String userId = payload.getSubject();
            UUID userUuid = UserHelper.parseUUID(userId);

            // Look up the user
            userRepository.findById(userUuid).ifPresent(user -> {
                if (user.isEnabled()) {
                    // FIX: Pass the entire User object as the principal, and use its built-in authorities
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities() // This now correctly includes the "ROLE_" prefix!
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            });

        } catch (ExpiredJwtException e) {
            request.setAttribute("error", "Token has expired");
        } catch (MalformedJwtException e) {
            request.setAttribute("error", "Invalid token format");
        } catch (SignatureException e) {
            request.setAttribute("error", "Token signature invalid");
        } catch (JwtException e) {
            request.setAttribute("error", "Token validation failed");
        } catch (Exception e) {
            logger.error("Unexpected error in JWT filter", e);
            request.setAttribute("error", "Authentication error");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/v1/auth");
    }
}