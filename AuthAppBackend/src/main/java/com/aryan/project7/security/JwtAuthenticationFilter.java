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

            // In JJWT 0.13.0, parsing returns Jws<Claims>
            Jws<Claims> jwsClaims = jwtService.parse(token);
            Claims payload = jwsClaims.getPayload();
            String userId = payload.getSubject();
            UUID userUuid = UserHelper.parseUUID(userId);

            userRepository.findById(userUuid).ifPresent(user -> {
                if (user.isEnabled()) {
                    List<GrantedAuthority> authorities = user.getRoles() == null ?
                            List.of() :
                            user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                                    .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            authorities
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            });

        } catch (ExpiredJwtException e) {
            request.setAttribute("error", "Token Expired");
        } catch (MalformedJwtException e) {
            request.setAttribute("error", "Invalid Token");
        } catch (Exception e) {
            request.setAttribute("error", "Authentication Failed");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/v1/auth");
    }
}