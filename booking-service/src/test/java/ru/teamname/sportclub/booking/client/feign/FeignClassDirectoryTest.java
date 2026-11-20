package ru.teamname.sportclub.booking.client.feign;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import ru.teamname.sportclub.booking.client.ClassInfo;
import ru.teamname.sportclub.booking.exception.ResourceNotFoundException;
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeignClassDirectoryTest {

    private final CatalogClient client = mock(CatalogClient.class);
    private final FeignClassDirectory directory = new FeignClassDirectory(client);

    @Test
    void getClassInfoMapsResponse() {
        LocalDateTime startsAt = LocalDateTime.of(2030, 1, 1, 10, 0);
        when(client.getClass(5L)).thenReturn(new ClassView(5L, "Yoga", startsAt, 12));

        ClassInfo info = directory.getClassInfo(5L);

        assertThat(info.id()).isEqualTo(5L);
        assertThat(info.title()).isEqualTo("Yoga");
        assertThat(info.startsAt()).isEqualTo(startsAt);
        assertThat(info.capacity()).isEqualTo(12);
    }

    @Test
    void turns404IntoResourceNotFound() {
        when(client.getClass(9L)).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> directory.getClassInfo(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Занятие");
    }

    @Test
    void turnsOtherFailuresIntoServiceUnavailable() {
        when(client.getClass(5L)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> directory.getClassInfo(5L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("catalog-service");
    }
}
