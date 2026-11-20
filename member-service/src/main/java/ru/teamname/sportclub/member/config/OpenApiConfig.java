package ru.teamname.sportclub.member.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 3 документация member-service (общий Swagger UI собирает gateway). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI memberServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Member Service API")
                .version("1.0.0")
                .description("Участники клуба, тарифы и абонементы."));
    }
}
