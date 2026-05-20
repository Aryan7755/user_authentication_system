package com.aryan.project7.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Java Auth App By Aryan",
                description = "Generic auth app to use with any application.",
                contact = @Contact(
                        name = "Aryan Raj",
                        email = "aryanbanthu@gmail.com"
                ),
                version = "1.0",
                summary = "Use this app to avoid building auth app based on jwt and oauth from scratch."

        ),
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)

@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class ApiDocConfig {
}
