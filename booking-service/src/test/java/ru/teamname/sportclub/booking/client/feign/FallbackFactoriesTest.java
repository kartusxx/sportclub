package ru.teamname.sportclub.booking.client.feign;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Circuit Breaker: при отказе зависимости (таймаут, разомкнутая цепь) клиент получает 503, а не 500. */
class FallbackFactoriesTest {

    private final MemberClientFallbackFactory memberFactory = new MemberClientFallbackFactory();
    private final CatalogClientFallbackFactory catalogFactory = new CatalogClientFallbackFactory();

    @Test
    void memberTimeoutBecomesServiceUnavailableForBothOperations() {
        MemberClient fallback = memberFactory.create(new TimeoutException("5s"));

        assertThatThrownBy(() -> fallback.getMember(1L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("member-service");
        assertThatThrownBy(() -> fallback.coverage(1L, "2026-12-01"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void catalogFailureBecomesServiceUnavailable() {
        CatalogClient fallback = catalogFactory.create(new IllegalStateException("circuit open"));

        assertThatThrownBy(() -> fallback.getClass(1L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("catalog-service");
    }

    @Test
    void wrappedFailuresAreUnwrapped() {
        MemberClient viaExecution = memberFactory.create(new ExecutionException(new IllegalStateException("x")));
        MemberClient viaCompletion = memberFactory.create(new CompletionException(new TimeoutException("x")));

        assertThatThrownBy(() -> viaExecution.getMember(1L)).isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> viaCompletion.getMember(1L)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void clientErrorIsPassedThroughSoThat404StaysA404() {
        FeignException.FeignClientException notFound = mock(FeignException.FeignClientException.class);
        MemberClient fallback = memberFactory.create(notFound);

        assertThatThrownBy(() -> fallback.getMember(1L)).isSameAs(notFound);
    }

    @Test
    void clientErrorInsideWrapperIsAlsoPassedThrough() {
        FeignException.FeignClientException notFound = mock(FeignException.FeignClientException.class);
        CatalogClient fallback = catalogFactory.create(new ExecutionException(notFound));

        assertThatThrownBy(() -> fallback.getClass(1L)).isSameAs(notFound);
    }
}
