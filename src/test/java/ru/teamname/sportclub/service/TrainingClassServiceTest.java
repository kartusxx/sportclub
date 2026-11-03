package ru.teamname.sportclub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import ru.teamname.sportclub.dto.SliceResponse;
import ru.teamname.sportclub.dto.TrainingClassRequest;
import ru.teamname.sportclub.dto.TrainingClassResponse;
import ru.teamname.sportclub.entity.Discipline;
import ru.teamname.sportclub.entity.Trainer;
import ru.teamname.sportclub.entity.TrainingClass;
import ru.teamname.sportclub.enums.BookingStatus;
import ru.teamname.sportclub.enums.ClassLevel;
import ru.teamname.sportclub.exception.BusinessRuleException;
import ru.teamname.sportclub.exception.ResourceNotFoundException;
import ru.teamname.sportclub.repository.BookingRepository;
import ru.teamname.sportclub.repository.DisciplineRepository;
import ru.teamname.sportclub.repository.TrainerRepository;
import ru.teamname.sportclub.repository.TrainingClassRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainingClassServiceTest {

    @Mock
    private TrainingClassRepository classRepository;
    @Mock
    private TrainerRepository trainerRepository;
    @Mock
    private DisciplineRepository disciplineRepository;
    @Mock
    private BookingRepository bookingRepository;

    private TrainingClassService service;
    private TrainingClass trainingClass;
    private Trainer trainer;
    private Discipline discipline;

    @BeforeEach
    void setUp() {
        service = new TrainingClassService(classRepository, trainerRepository, disciplineRepository,
                bookingRepository);

        trainer = new Trainer();
        trainer.setId(3L);
        trainer.setFirstName("Anna");
        trainer.setLastName("Smirnova");

        discipline = new Discipline();
        discipline.setId(4L);
        discipline.setName("Yoga");

        trainingClass = new TrainingClass();
        trainingClass.setId(10L);
        trainingClass.setTitle("Morning yoga");
        trainingClass.setTrainer(trainer);
        trainingClass.setDiscipline(discipline);
        trainingClass.setCapacity(10);
        trainingClass.setLevel(ClassLevel.BEGINNER);

        when(classRepository.findById(10L)).thenReturn(Optional.of(trainingClass));
        when(trainerRepository.findById(3L)).thenReturn(Optional.of(trainer));
        when(disciplineRepository.findById(4L)).thenReturn(Optional.of(discipline));
        when(classRepository.save(any(TrainingClass.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classRepository.saveAndFlush(any(TrainingClass.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TrainingClassRequest request(long trainerId, long disciplineId, int capacity) {
        return new TrainingClassRequest("Evening yoga", "desc", trainerId, disciplineId, "Hall 2",
                LocalDateTime.now().plusDays(2), 60, capacity, ClassLevel.ADVANCED);
    }

    @Test
    void deleteRemovesBookingsBeforeTheClass() {
        service.delete(10L);

        InOrder order = inOrder(bookingRepository, classRepository);
        order.verify(bookingRepository).deleteByTrainingClassId(10L);
        order.verify(classRepository).delete(trainingClass);
        order.verify(classRepository).flush();
    }

    @Test
    void deleteFailsForUnknownClassWithoutTouchingBookings() {
        when(classRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(bookingRepository, never()).deleteByTrainingClassId(any());
    }

    @Test
    void updateRejectsCapacityBelowBookedCount() {
        when(bookingRepository.countByTrainingClassIdAndStatus(10L, BookingStatus.BOOKED)).thenReturn(5L);

        assertThatThrownBy(() -> service.update(10L, request(3L, 4L, 4)))
                .isInstanceOf(BusinessRuleException.class);
        verify(classRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateAppliesNewValues() {
        when(bookingRepository.countByTrainingClassIdAndStatus(10L, BookingStatus.BOOKED)).thenReturn(2L);

        TrainingClassResponse response = service.update(10L, request(3L, 4L, 20));

        assertThat(response.title()).isEqualTo("Evening yoga");
        assertThat(response.capacity()).isEqualTo(20);
        assertThat(response.level()).isEqualTo(ClassLevel.ADVANCED);
        assertThat(response.trainerName()).isEqualTo("Anna Smirnova");
    }

    @Test
    void createFailsForUnknownTrainerOrDiscipline() {
        assertThatThrownBy(() -> service.create(request(99L, 4L, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.create(request(3L, 99L, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(classRepository, never()).save(any());
    }

    @Test
    void createSavesClass() {
        TrainingClassResponse response = service.create(request(3L, 4L, 12));

        assertThat(response.disciplineName()).isEqualTo("Yoga");
        assertThat(response.capacity()).isEqualTo(12);
    }

    @Test
    void feedMapsSliceWithoutTotalCount() {
        PageRequest pageable = PageRequest.of(0, 1);
        when(classRepository.findBy(pageable)).thenReturn(new SliceImpl<>(List.of(trainingClass), pageable, true));

        SliceResponse<TrainingClassResponse> feed = service.findFeed(pageable);

        assertThat(feed.content()).hasSize(1);
        assertThat(feed.hasNext()).isTrue();
        assertThat(feed.page()).isZero();
        assertThat(feed.size()).isEqualTo(1);
    }

    @Test
    void findByIdReturnsClassOrFails() {
        assertThat(service.findById(10L).id()).isEqualTo(10L);

        when(classRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(404L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
