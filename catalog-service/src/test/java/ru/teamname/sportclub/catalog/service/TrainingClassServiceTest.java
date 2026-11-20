package ru.teamname.sportclub.catalog.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.teamname.sportclub.catalog.client.BookingGateway;
import ru.teamname.sportclub.catalog.dto.TrainingClassRequest;
import ru.teamname.sportclub.catalog.entity.Discipline;
import ru.teamname.sportclub.catalog.entity.Trainer;
import ru.teamname.sportclub.catalog.entity.TrainingClass;
import ru.teamname.sportclub.catalog.enums.ClassLevel;
import ru.teamname.sportclub.catalog.exception.ConflictException;
import ru.teamname.sportclub.catalog.exception.ResourceNotFoundException;
import ru.teamname.sportclub.catalog.exception.ServiceUnavailableException;
import ru.teamname.sportclub.catalog.repository.DisciplineRepository;
import ru.teamname.sportclub.catalog.repository.TrainerRepository;
import ru.teamname.sportclub.catalog.repository.TrainingClassRepository;
import ru.teamname.sportclub.catalog.support.EntityValidator;
import ru.teamname.sportclub.catalog.support.PagedReader;
import ru.teamname.sportclub.catalog.web.PageQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrainingClassServiceTest {

    private final TrainingClassRepository classes = mock(TrainingClassRepository.class);
    private final TrainerRepository trainers = mock(TrainerRepository.class);
    private final DisciplineRepository disciplines = mock(DisciplineRepository.class);
    private final PagedReader pagedReader = mock(PagedReader.class);
    private final BookingGateway bookingGateway = mock(BookingGateway.class);
    private final TrainingClassService service = new TrainingClassService(classes, trainers, disciplines,
            pagedReader, new EntityValidator(), bookingGateway);

    private static Trainer trainer() {
        Trainer trainer = new Trainer();
        trainer.setId(1L);
        trainer.setFirstName("Anna");
        trainer.setLastName("Smirnova");
        trainer.setEmail("anna@example.com");
        trainer.setExperienceYears(5);
        return trainer;
    }

    private static Discipline discipline() {
        Discipline discipline = new Discipline();
        discipline.setId(2L);
        discipline.setName("Йога");
        return discipline;
    }

    private static TrainingClass trainingClass(long id, int capacity) {
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setId(id);
        trainingClass.setTitle("Утренняя йога");
        trainingClass.setTrainerId(1L);
        trainingClass.setDisciplineId(2L);
        trainingClass.setRoom("Зал 1");
        trainingClass.setStartsAt(LocalDateTime.now().plusDays(2));
        trainingClass.setDurationMinutes(60);
        trainingClass.setCapacity(capacity);
        trainingClass.setLevel(ClassLevel.BEGINNER);
        return trainingClass;
    }

    private static TrainingClassRequest request(int capacity) {
        return new TrainingClassRequest("Утренняя йога", null, 1L, 2L, "Зал 1",
                LocalDateTime.now().plusDays(2), 60, capacity, ClassLevel.BEGINNER);
    }

    /** Тренер и направление существуют: именно их подтягивают create и update. */
    private void referencesExist() {
        when(trainers.findById(1L)).thenReturn(Mono.just(trainer()));
        when(disciplines.findById(2L)).thenReturn(Mono.just(discipline()));
    }

    private void saveReturnsArgument() {
        when(classes.save(any(TrainingClass.class))).thenAnswer(call -> {
            TrainingClass saved = call.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(100L);
            }
            return Mono.just(saved);
        });
    }

    @Test
    void createSavesClassAndFillsNames() {
        referencesExist();
        saveReturnsArgument();

        StepVerifier.create(service.create(request(10)))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(100L);
                    assertThat(response.trainerName()).isEqualTo("Anna Smirnova");
                    assertThat(response.disciplineName()).isEqualTo("Йога");
                    assertThat(response.capacity()).isEqualTo(10);
                })
                .verifyComplete();
    }

    @Test
    void createFailsWhenTrainerDoesNotExist() {
        when(trainers.findById(1L)).thenReturn(Mono.empty());
        when(disciplines.findById(2L)).thenReturn(Mono.just(discipline()));

        StepVerifier.create(service.create(request(10)))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Тренер"))
                .verify();
        verify(classes, never()).save(any());
    }

    @Test
    void createFailsWhenDisciplineDoesNotExist() {
        when(trainers.findById(1L)).thenReturn(Mono.just(trainer()));
        when(disciplines.findById(2L)).thenReturn(Mono.empty());

        StepVerifier.create(service.create(request(10)))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Направление"))
                .verify();
    }

    @Test
    void findByIdReturnsClassWithNames() {
        when(classes.findById(5L)).thenReturn(Mono.just(trainingClass(5, 10)));
        when(trainers.findAllById(anyIterable())).thenReturn(Flux.just(trainer()));
        when(disciplines.findAllById(anyIterable())).thenReturn(Flux.just(discipline()));

        StepVerifier.create(service.findById(5L))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(5L);
                    assertThat(response.trainerName()).isEqualTo("Anna Smirnova");
                })
                .verifyComplete();
    }

    @Test
    void findByIdOfUnknownClassIsNotFound() {
        when(classes.findById(anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(404L)).expectError(ResourceNotFoundException.class).verify();
    }

    @Test
    void feedKeepsHasNextFlagAndLoadsNamesInBatch() {
        PageQuery query = PageQuery.of(0, 2, null, "startsAt", Set.of("startsAt"));
        Slice<TrainingClass> slice = new SliceImpl<>(List.of(trainingClass(1, 10), trainingClass(2, 10)),
                query.toPageable(), true);
        when(pagedReader.readSlice(eq(TrainingClass.class), any(PageQuery.class)))
                .thenReturn(Mono.<Slice<TrainingClass>>just(slice));
        when(trainers.findAllById(anyIterable())).thenReturn(Flux.just(trainer()));
        when(disciplines.findAllById(anyIterable())).thenReturn(Flux.just(discipline()));

        StepVerifier.create(service.findFeed(query))
                .assertNext(feed -> {
                    assertThat(feed.content()).hasSize(2);
                    assertThat(feed.hasNext()).isTrue();
                    assertThat(feed.size()).isEqualTo(2);
                })
                .verifyComplete();

        // имена тренеров и направлений загружаются по одному запросу на всю пачку (нет N+1)
        verify(trainers).findAllById(anyIterable());
        verify(disciplines).findAllById(anyIterable());
    }

    @Test
    void emptyFeedDoesNotQueryReferences() {
        PageQuery query = PageQuery.of(0, 2, null, "startsAt", Set.of("startsAt"));
        when(pagedReader.readSlice(eq(TrainingClass.class), any(PageQuery.class)))
                .thenReturn(Mono.<Slice<TrainingClass>>just(new SliceImpl<>(List.of(), query.toPageable(), false)));

        StepVerifier.create(service.findFeed(query))
                .assertNext(feed -> {
                    assertThat(feed.content()).isEmpty();
                    assertThat(feed.hasNext()).isFalse();
                })
                .verifyComplete();
        verifyNoInteractions(trainers, disciplines);
    }

    @Test
    void updateRejectsCapacityBelowBookedCount() {
        when(classes.findById(5L)).thenReturn(Mono.just(trainingClass(5, 10)));
        referencesExist();
        when(bookingGateway.bookedCount(5L)).thenReturn(Mono.just(6L));

        StepVerifier.create(service.update(5L, request(5)))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ConflictException.class).hasMessageContaining("6"))
                .verify();
        verify(classes, never()).save(any());
    }

    @Test
    void updateAllowsShrinkingToBookedCount() {
        when(classes.findById(5L)).thenReturn(Mono.just(trainingClass(5, 10)));
        referencesExist();
        saveReturnsArgument();
        when(bookingGateway.bookedCount(5L)).thenReturn(Mono.just(5L));

        StepVerifier.create(service.update(5L, request(5)))
                .assertNext(response -> assertThat(response.capacity()).isEqualTo(5))
                .verifyComplete();
    }

    @Test
    void updateDoesNotAskBookingServiceWhenCapacityGrows() {
        when(classes.findById(5L)).thenReturn(Mono.just(trainingClass(5, 10)));
        referencesExist();
        saveReturnsArgument();

        StepVerifier.create(service.update(5L, request(20)))
                .assertNext(response -> assertThat(response.capacity()).isEqualTo(20))
                .verifyComplete();

        verifyNoInteractions(bookingGateway);
    }

    @Test
    void updateFailsWhenBookingServiceIsUnavailable() {
        when(classes.findById(5L)).thenReturn(Mono.just(trainingClass(5, 10)));
        referencesExist();
        when(bookingGateway.bookedCount(5L)).thenReturn(Mono.error(new ServiceUnavailableException("недоступен")));

        StepVerifier.create(service.update(5L, request(5)))
                .expectError(ServiceUnavailableException.class)
                .verify();
        verify(classes, never()).save(any());
    }

    @Test
    void updateOfUnknownClassIsNotFound() {
        when(classes.findById(5L)).thenReturn(Mono.empty());

        StepVerifier.create(service.update(5L, request(5)))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void deleteRemovesClassBeforeCleaningBookings() {
        TrainingClass existing = trainingClass(5, 10);
        when(classes.findById(5L)).thenReturn(Mono.just(existing));
        when(classes.delete(existing)).thenReturn(Mono.empty());
        when(bookingGateway.deleteBookings(5L)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(5L)).verifyComplete();

        var order = inOrder(classes, bookingGateway);
        order.verify(classes).delete(existing);
        order.verify(bookingGateway).deleteBookings(5L);
    }

    @Test
    void deleteOfUnknownClassDoesNotTouchBookings() {
        when(classes.findById(5L)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(5L)).expectError(ResourceNotFoundException.class).verify();

        verifyNoInteractions(bookingGateway);
    }
}
