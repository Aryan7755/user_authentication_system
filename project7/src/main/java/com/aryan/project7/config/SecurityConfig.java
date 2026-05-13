package com.aryan.project7.config;

import com.aryan.project7.dtos.ApiError;
import com.aryan.project7.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    // Standard constructor injection for our filter and success handler
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, AuthenticationSuccessHandler authenticationSuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // We're disabling CSRF because we're using JWTs (and it's a headache for APIs anyway)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // Since it's a REST API, we don't want sessions—keep it stateless
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Using that list we made in AppConstants to let people see the docs and login
                        .requestMatchers(AppConstants.AUTH_PUBLIC_URLS).permitAll()
                        // Everything else requires a valid token
                        .anyRequest().authenticated()
                )

                // Handling social logins (Google, GitHub, etc.)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(null)) // You might want to add a failure handler here later!

                .logout(AbstractHttpConfigurer::disable)

                // This is our "Oops, you're not allowed here" custom response logic
                .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");

                    String message = authException.getMessage();
                    String error = (String) request.getAttribute("error");

                    // If we set a specific error in the request, use that instead of the generic one
                    if (error != null) {
                        message = error;
                    }

                    // Sending back a nice JSON object instead of a messy stack trace
                    var apiError = ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized Access", message, request.getRequestURI(), true);
                    var objectMapper = new ObjectMapper();
                    response.getWriter().write(objectMapper.writeValueAsString(apiError));
                }))

                // We need to run our JWT check BEFORE the standard username/password check
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    // Standard bean for hashing passwords—never store them in plain text!
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // This helps Spring manage the actual authentication process
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}