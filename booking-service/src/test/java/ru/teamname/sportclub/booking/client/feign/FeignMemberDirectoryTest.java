package ru.teamname.sportclub.booking.client.feign;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import ru.teamname.sportclub.booking.client.MemberInfo;
import ru.teamname.sportclub.booking.exception.ResourceNotFoundException;
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeignMemberDirectoryTest {

    private final MemberClient client = mock(MemberClient.class);
    private final FeignMemberDirectory directory = new FeignMemberDirectory(client);

    @Test
    void getMemberMapsResponse() {
        when(client.getMember(1L)).thenReturn(new MemberView(1L, "Ivan Petrov", "ACTIVE"));

        MemberInfo member = directory.getMember(1L);

        assertThat(member.id()).isEqualTo(1L);
        assertThat(member.fullName()).isEqualTo("Ivan Petrov");
        assertThat(member.isActive()).isTrue();
    }

    @Test
    void memberWithAnotherStatusIsNotActive() {
        when(client.getMember(1L)).thenReturn(new MemberView(1L, "Ivan Petrov", "FROZEN"));

        assertThat(directory.getMember(1L).isActive()).isFalse();
    }

    @Test
    void getMemberTurns404IntoResourceNotFound() {
        when(client.getMember(9L)).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> directory.getMember(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9");
    }

    @Test
    void getMemberTurnsOtherFailuresIntoServiceUnavailable() {
        when(client.getMember(1L)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> directory.getMember(1L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("member-service");
    }

    @Test
    void coverageIsAskedWithIsoDate() {
        when(client.coverage(1L, "2026-12-01")).thenReturn(new CoverageView(true));
        when(client.coverage(2L, "2026-12-01")).thenReturn(new CoverageView(false));

        assertThat(directory.hasActiveSubscription(1L, LocalDate.of(2026, 12, 1))).isTrue();
        assertThat(directory.hasActiveSubscription(2L, LocalDate.of(2026, 12, 1))).isFalse();
    }

    @Test
    void coverageTurns404IntoResourceNotFound() {
        when(client.coverage(9L, "2026-12-01")).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> directory.hasActiveSubscription(9L, LocalDate.of(2026, 12, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void coverageTurnsOtherFailuresIntoServiceUnavailable() {
        when(client.coverage(1L, "2026-12-01")).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> directory.hasActiveSubscription(1L, LocalDate.of(2026, 12, 1)))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
