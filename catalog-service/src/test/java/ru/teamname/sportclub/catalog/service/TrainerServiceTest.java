package ru.teamname.sportclub.catalog.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.teamname.sportclub.catalog.dto.TrainerRequest;
import ru.teamname.sportclub.catalog.entity.Discipline;
import ru.teamname.sportclub.catalog.entity.Trainer;
import ru.teamname.sportclub.catalog.exception.ConflictException;
import ru.teamname.sportclub.catalog.exception.ResourceNotFoundException;
import ru.teamname.sportclub.catalog.repository.DisciplineRepository;
import ru.teamname.sportclub.catalog.repository.TrainerDisciplineRepository;
import ru.teamname.sportclub.catalog.repository.TrainerRepository;
import ru.teamname.sportclub.catalog.support.EntityValidator;
import ru.teamname.sportclub.catalog.support.PagedReader;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainerServiceTest {

    private final TrainerRepository trainers = mock(TrainerRepository.class);
    private final DisciplineRepository disciplines = mock(DisciplineRepository.class);
    private final TrainerDisciplineRepository links = mock(TrainerDisciplineRepository.class);
    private final TrainerService service = new TrainerService(trainers, disciplines, links,
            mock(PagedReader.class), new EntityValidator());

    private static TrainerRequest request(String email, Set<Long> disciplineIds) {
        return new TrainerRequest("Anna", "Smirnova", email, 5, "bio", disciplineIds);
    }

    private static Discipline discipline(long id, String name) {
        Discipline discipline = new Discipline();
        discipline.setId(id);
        discipline.setName(name);
        return discipline;
    }

    private static Trainer trainer(long id) {
        Trainer trainer = new Trainer();
        trainer.setId(id);
        trainer.setFirstName("Anna");
        trainer.setLastName("Smirnova");
        trainer.setEmail("anna@example.com");
        trainer.setExperienceYears(5);
        return trainer;
    }

    private void saveAssignsId(long id) {
        when(trainers.save(any(Trainer.class))).thenAnswer(call -> {
            Trainer saved = call.getArgument(0);
            saved.setId(id);
            return Mono.just(saved);
        });
    }

    @Test
    void createSavesTrainerAndReplacesDisciplineLinks() {
        when(trainers.countByEmailIgnoreCase("anna@example.com")).thenReturn(Mono.just(0L));
        when(disciplines.findAllById(anyIterable()))
                .thenReturn(Flux.just(discipline(1, "Йога"), discipline(2, "Бокс")));
        saveAssignsId(7L);
        when(links.replace(eq(7L), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(request("anna@example.com", Set.of(1L, 2L))))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(7L);
                    // направления в ответе отсортированы по названию
                    assertThat(response.disciplines()).extracting("name").containsExactly("Бокс", "Йога");
                })
                .verifyComplete();

        verify(links).replace(eq(7L), eq(Set.of(1L, 2L)));
    }

    @Test
    void createWithoutDisciplinesSkipsLookup() {
        when(trainers.countByEmailIgnoreCase("anna@example.com")).thenReturn(Mono.just(0L));
        saveAssignsId(7L);
        when(links.replace(eq(7L), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(request("anna@example.com", null)))
                .assertNext(response -> assertThat(response.disciplines()).isEmpty())
                .verifyComplete();

        verify(disciplines, never()).findAllById(anyIterable());
    }

    @Test
    void createFailsWhenEmailIsTaken() {
        when(trainers.countByEmailIgnoreCase("anna@example.com")).thenReturn(Mono.just(1L));

        StepVerifier.create(service.create(request("anna@example.com", Set.of())))
                .expectError(ConflictException.class)
                .verify();
        verify(trainers, never()).save(any());
    }

    @Test
    void createFailsWhenDisciplineDoesNotExist() {
        when(trainers.countByEmailIgnoreCase("anna@example.com")).thenReturn(Mono.just(0L));
        when(disciplines.findAllById(anyIterable())).thenReturn(Flux.just(discipline(1, "Йога")));

        StepVerifier.create(service.create(request("anna@example.com", Set.of(1L, 99L))))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("99"))
                .verify();
        verify(trainers, never()).save(any());
    }

    @Test
    void updateIgnoresOwnEmailAndRewritesLinks() {
        when(trainers.findById(7L)).thenReturn(Mono.just(trainer(7)));
        when(trainers.countByEmailIgnoreCaseAndIdNot("anna@example.com", 7L)).thenReturn(Mono.just(0L));
        when(trainers.save(any(Trainer.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));
        when(links.replace(eq(7L), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.update(7L, request("anna@example.com", Set.of())))
                .assertNext(response -> assertThat(response.id()).isEqualTo(7L))
                .verifyComplete();

        verify(links).replace(eq(7L), eq(Set.of()));
    }

    @Test
    void updateFailsWhenEmailBelongsToAnotherTrainer() {
        when(trainers.findById(7L)).thenReturn(Mono.just(trainer(7)));
        when(trainers.countByEmailIgnoreCaseAndIdNot("busy@example.com", 7L)).thenReturn(Mono.just(1L));

        StepVerifier.create(service.update(7L, request("busy@example.com", Set.of())))
                .expectError(ConflictException.class)
                .verify();
    }

    @Test
    void findByIdAttachesDisciplines() {
        when(trainers.findById(7L)).thenReturn(Mono.just(trainer(7)));
        when(links.findDisciplinesByTrainerIds(any()))
                .thenReturn(Mono.just(Map.of(7L, List.of(discipline(1, "Йога")))));

        StepVerifier.create(service.findById(7L))
                .assertNext(response -> assertThat(response.disciplines()).hasSize(1))
                .verifyComplete();
    }

    @Test
    void findByIdOfUnknownTrainerIsNotFound() {
        when(trainers.findById(anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(404L)).expectError(ResourceNotFoundException.class).verify();
    }

    @Test
    void deleteRemovesTrainer() {
        Trainer existing = trainer(7);
        when(trainers.findById(7L)).thenReturn(Mono.just(existing));
        when(trainers.delete(existing)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(7L)).verifyComplete();

        verify(trainers).delete(existing);
    }

    @Test
    void deleteOfUnknownTrainerIsNotFound() {
        when(trainers.findById(8L)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(8L)).expectError(ResourceNotFoundException.class).verify();
    }
}
