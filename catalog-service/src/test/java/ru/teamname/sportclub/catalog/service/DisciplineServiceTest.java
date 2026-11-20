package ru.teamname.sportclub.catalog.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.teamname.sportclub.catalog.dto.DisciplineRequest;
import ru.teamname.sportclub.catalog.entity.Discipline;
import ru.teamname.sportclub.catalog.exception.ConflictException;
import ru.teamname.sportclub.catalog.exception.ResourceNotFoundException;
import ru.teamname.sportclub.catalog.repository.DisciplineRepository;
import ru.teamname.sportclub.catalog.support.EntityValidator;
import ru.teamname.sportclub.catalog.support.PagedReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisciplineServiceTest {

    private final DisciplineRepository repository = mock(DisciplineRepository.class);
    private final DisciplineService service =
            new DisciplineService(repository, mock(PagedReader.class), new EntityValidator());

    private static Discipline discipline(long id, String name) {
        Discipline discipline = new Discipline();
        discipline.setId(id);
        discipline.setName(name);
        return discipline;
    }

    @Test
    void createSavesDisciplineWhenNameIsFree() {
        when(repository.countByName("Йога")).thenReturn(Mono.just(0L));
        when(repository.save(any(Discipline.class))).thenAnswer(call -> {
            Discipline saved = call.getArgument(0);
            saved.setId(10L);
            return Mono.just(saved);
        });

        StepVerifier.create(service.create(new DisciplineRequest("Йога", "Растяжка")))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(10L);
                    assertThat(response.name()).isEqualTo("Йога");
                })
                .verifyComplete();
    }

    @Test
    void createFailsWhenNameIsTaken() {
        when(repository.countByName("Йога")).thenReturn(Mono.just(1L));

        StepVerifier.create(service.create(new DisciplineRequest("Йога", null)))
                .expectError(ConflictException.class)
                .verify();
        verify(repository, never()).save(any());
    }

    @Test
    void updateChecksNameAgainstOtherDisciplines() {
        when(repository.findById(1L)).thenReturn(Mono.just(discipline(1, "Йога")));
        when(repository.countByNameExcludingId("Пилатес", 1L)).thenReturn(Mono.just(1L));

        StepVerifier.create(service.update(1L, new DisciplineRequest("Пилатес", null)))
                .expectError(ConflictException.class)
                .verify();
    }

    @Test
    void updateSavesNewValues() {
        when(repository.findById(1L)).thenReturn(Mono.just(discipline(1, "Йога")));
        when(repository.countByNameExcludingId("Пилатес", 1L)).thenReturn(Mono.just(0L));
        when(repository.save(any(Discipline.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        StepVerifier.create(service.update(1L, new DisciplineRequest("Пилатес", "Новое")))
                .assertNext(response -> assertThat(response.name())
                        .isEqualTo("Пилатес"))
                .verifyComplete();
    }

    @Test
    void findByIdThrowsNotFound() {
        when(repository.findById(anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(99L))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("99"))
                .verify();
    }

    @Test
    void deleteRemovesExistingDiscipline() {
        Discipline existing = discipline(1, "Йога");
        when(repository.findById(1L)).thenReturn(Mono.just(existing));
        when(repository.delete(existing)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(1L)).verifyComplete();

        verify(repository).delete(existing);
    }

    @Test
    void deleteOfUnknownDisciplineIsNotFound() {
        when(repository.findById(2L)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(2L)).expectError(ResourceNotFoundException.class).verify();
    }
}
