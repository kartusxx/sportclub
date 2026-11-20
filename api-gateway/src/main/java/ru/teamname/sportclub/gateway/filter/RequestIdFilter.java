package ru.teamname.sportclub.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Сквозной идентификатор запроса: берётся из заголовка X-Request-Id клиента или генерируется,
 * передаётся сервисам, возвращается в ответе и пишется в лог вместе с методом, путём, статусом и временем.
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    public static final String HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String requestId = incoming == null || incoming.isBlank() ? UUID.randomUUID().toString() : incoming;
        long startedAt = System.nanoTime();

        ServerWebExchange mutated = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(HEADER, requestId)))
                .build();
        mutated.getResponse().getHeaders().set(HEADER, requestId);

        return chain.filter(mutated).doFinally(signal -> log.info("{} {} {} -> {} за {} мс",
                requestId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getRawPath(),
                mutated.getResponse().getStatusCode(),
                (System.nanoTime() - startedAt) / 1_000_000));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
