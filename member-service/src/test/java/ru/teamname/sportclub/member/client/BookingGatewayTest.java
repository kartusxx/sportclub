package ru.teamname.sportclub.member.client;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import ru.teamname.sportclub.member.exception.ServiceUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingGatewayTest {

    private final BookingClient client = mock(BookingClient.class);
    private final BookingGateway gateway = new BookingGateway(client);

    @Test
    void returnsAnswerOfBookingService() {
        when(client.memberHasBookings(1L)).thenReturn(new BookingClient.ExistsView(true));
        when(client.memberHasBookings(2L)).thenReturn(new BookingClient.ExistsView(false));

        assertThat(gateway.hasBookings(1L)).isTrue();
        assertThat(gateway.hasBookings(2L)).isFalse();
    }

    @Test
    void translatesFeignFailureToServiceUnavailable() {
        when(client.memberHasBookings(1L)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> gateway.hasBookings(1L)).isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("booking-service");
    }
}
