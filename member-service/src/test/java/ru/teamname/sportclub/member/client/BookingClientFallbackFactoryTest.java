package ru.teamname.sportclub.member.client;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import ru.teamname.sportclub.member.exception.ServiceUnavailableException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Circuit Breaker: при отказе зависимости (таймаут, разомкнутая цепь) клиент получает 503, а не 500. */
class BookingClientFallbackFactoryTest {

    private final BookingClientFallbackFactory factory = new BookingClientFallbackFactory();

    @Test
    void timeoutBecomesServiceUnavailable() {
        BookingClient fallback = factory.create(new TimeoutException("5s"));

        assertThatThrownBy(() -> fallback.memberHasBookings(1L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("booking-service");
    }

    @Test
    void wrappedFailureIsUnwrappedAndBecomesServiceUnavailable() {
        BookingClient fallback = factory.create(new ExecutionException(new IllegalStateException("circuit open")));

        assertThatThrownBy(() -> fallback.memberHasBookings(1L)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void clientErrorIsPassedThroughUnchanged() {
        FeignException.FeignClientException notFound = mock(FeignException.FeignClientException.class);
        BookingClient fallback = factory.create(notFound);

        assertThatThrownBy(() -> fallback.memberHasBookings(1L)).isSameAs(notFound);
    }
}
