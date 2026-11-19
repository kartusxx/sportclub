package ru.teamname.sportclub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Часы вынесены в бин, чтобы бизнес-логика, зависящая от времени, легко тестировалась. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
