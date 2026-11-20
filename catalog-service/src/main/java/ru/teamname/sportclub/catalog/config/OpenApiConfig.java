package ru.teamname.sportclub.catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 3 документация catalog-service (общий Swagger UI собирает gateway). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Catalog Service API")
                .version("1.0.0")
                .description("Тренеры, направления и расписание занятий."));
    }
}
