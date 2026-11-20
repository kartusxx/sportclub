package ru.teamname.sportclub.member.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/** Включает Feign-клиенты. Вынесено в отдельный класс, чтобы не менять главный класс приложения. */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackages = "ru.teamname.sportclub.member.client")
public class FeignConfig {
}
