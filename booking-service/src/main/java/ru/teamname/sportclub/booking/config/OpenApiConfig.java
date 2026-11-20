package ru.teamname.sportclub.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** OpenAPI 3 документация booking-service (общий Swagger UI собирает gateway). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingServiceOpenApi() {
        return new OpenAPI()
                // Относительный адрес: «Try it out» вызывает тот хост, где открыт Swagger UI, то есть gateway
                .servers(List.of(new Server().url("/")))
                .info(new Info()
                .title("Booking Service API")
                .version("1.0.0")
                .description("Записи участников на занятия."));
    }
}
