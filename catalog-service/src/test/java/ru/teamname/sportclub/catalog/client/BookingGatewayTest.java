package ru.teamname.sportclub.catalog.client;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.teamname.sportclub.catalog.exception.ServiceUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingGatewayTest {

    private final BookingClient client = mock(BookingClient.class);
    private final BookingGateway gateway = new BookingGateway(client);

    @Test
    void bookedCountReturnsAnswerOfBookingService() {
        when(client.bookedCount(1L)).thenReturn(new BookingClient.CountView(7));

        StepVerifier.create(gateway.bookedCount(1L)).expectNext(7L).verifyComplete();
    }

    @Test
    void bookedCountTranslatesFeignFailureToServiceUnavailable() {
        when(client.bookedCount(1L)).thenThrow(mock(FeignException.class));

        StepVerifier.create(gateway.bookedCount(1L))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ServiceUnavailableException.class)
                        .hasMessageContaining("booking-service"))
                .verify();
    }

    @Test
    void deleteBookingsCallsBookingService() {
        StepVerifier.create(gateway.deleteBookings(5L)).verifyComplete();

        verify(client).deleteByClass(5L);
    }

    @Test
    void deleteBookingsIsBestEffortAndSwallowsFailures() {
        doThrow(new IllegalStateException("booking-service down")).when(client).deleteByClass(5L);

        StepVerifier.create(gateway.deleteBookings(5L)).verifyComplete();
    }
}
