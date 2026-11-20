package ru.teamname.sportclub.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();
    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };

    private static MockServerWebExchange exchange(String requestId) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/api/members");
        if (requestId != null) {
            request.header(RequestIdFilter.HEADER, requestId);
        }
        return MockServerWebExchange.from(request);
    }

    @Test
    void keepsRequestIdOfTheClientAndPassesItOn() {
        MockServerWebExchange exchange = exchange("client-id-1");

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(RequestIdFilter.HEADER))
                .isEqualTo("client-id-1");
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER)).isEqualTo("client-id-1");
    }

    @Test
    void generatesRequestIdWhenClientSentNone() {
        MockServerWebExchange exchange = exchange(null);

        filter.filter(exchange, chain).block();

        String generated = exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER);
        assertThat(UUID.fromString(generated)).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(RequestIdFilter.HEADER)).isEqualTo(generated);
    }

    @Test
    void replacesBlankRequestId() {
        MockServerWebExchange exchange = exchange("  ");

        filter.filter(exchange, chain).block();

        String id = exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER);
        assertThat(id).isNotBlank();
        assertThat(UUID.fromString(id)).isNotNull();
    }

    @Test
    void responseKeepsRequestIdEvenWhenDownstreamFails() {
        MockServerWebExchange exchange = exchange("fail-1");
        GatewayFilterChain failing = ex -> Mono.error(new IllegalStateException("service down"));

        assertThatThrownBy(() -> filter.filter(exchange, failing).block())
                .isInstanceOf(IllegalStateException.class);

        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER)).isEqualTo("fail-1");
    }

    @Test
    void runsBeforeAllOtherFilters() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
