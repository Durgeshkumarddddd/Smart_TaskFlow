package com.taskflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TaskFlow API",
                version = "1.0",
                description = "TaskFlow backend API for task management, teams, time tracking, and analytics.",
                contact = @Contact(name = "TaskFlow", email = "support@taskflow.local"),
                license = @License(name = "MIT")
        )
)
// Additional OpenAPI customizations can be added here if needed.
public class OpenApiConfig {
}
