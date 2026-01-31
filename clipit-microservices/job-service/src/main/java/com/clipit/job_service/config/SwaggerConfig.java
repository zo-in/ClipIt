package com.clipit.job_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "ClipIt Job Service",
        version = "1.0",
        description = "API for downloading and processing media"
    )
)
public class SwaggerConfig {
}