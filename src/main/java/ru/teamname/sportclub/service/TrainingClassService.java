package ru.teamname.sportclub.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teamname.sportclub.dto.SliceResponse;
import ru.teamname.sportclub.dto.TrainingClassRequest;
import ru.teamname.sportclub.dto.TrainingClassResponse;
import ru.teamname.sportclub.entity.Discipline;
import ru.teamname.sportclub.entity.Trainer;
import ru.teamname.sportclub.entity.TrainingClass;
import ru.teamname.sportclub.enums.BookingStatus;
import ru.teamname.sportclub.exception.BusinessRuleException;
import ru.teamname.sportclub.exception.ResourceNotFoundException;
import ru.teamname.sportclub.mapper.TrainingClassMapper;
import ru.teamname.sportclub.repository.BookingRepository;
import ru.teamname.sportclub.repository.DisciplineRepository;
import ru.teamname.sportclub.repository.TrainerRepository;
import ru.teamname.sportclub.repository.TrainingClassRepository;

@Service
@Transactional(readOnly = true)
public class TrainingClassService {

    private final TrainingClassRepository classRepository;
    private final TrainerRepository trainerRepository;
    private final DisciplineRepository disciplineRepository;
    private final BookingRepository bookingRepository;

    public TrainingClassService(TrainingClassRepository classRepository,
                                TrainerRepository trainerRepository,
                                DisciplineRepository disciplineRepository,
                                BookingRepository bookingRepository) {
        this.classRepository = classRepository;
        this.trainerRepository = trainerRepository;
        this.disciplineRepository = disciplineRepository;
        this.bookingRepository = bookingRepository;
    }

    /** Лента занятий для бесконечной прокрутки: без подсчёта общего количества записей. */
    public SliceResponse<TrainingClassResponse> findFeed(Pageable pageable) {
        return SliceResponse.from(classRepository.findBy(pageable), TrainingClassMapper::toResponse);
    }

    public TrainingClassResponse findById(Long id) {
        return TrainingClassMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public TrainingClassResponse create(TrainingClassRequest request) {
        TrainingClass trainingClass = new TrainingClass();
        TrainingClassMapper.apply(request, trainingClass, getTrainer(request.trainerId()),
                getDiscipline(request.disciplineId()));
        return TrainingClassMapper.toResponse(classRepository.save(trainingClass));
    }

    @Transactional
    public TrainingClassResponse update(Long id, TrainingClassRequest request) {
        TrainingClass trainingClass = getOrThrow(id);
        long booked = bookingRepository.countByTrainingClassIdAndStatus(id, BookingStatus.BOOKED);
        if (request.capacity() < booked) {
            throw new BusinessRuleException("Вместимость не может быть меньше числа записанных участников ("
                    + booked + ")");
        }
        TrainingClassMapper.apply(request, trainingClass, getTrainer(request.trainerId()),
                getDiscipline(request.disciplineId()));
        return TrainingClassMapper.toResponse(classRepository.saveAndFlush(trainingClass));
    }

    /**
     * Удаление занятия затрагивает две таблицы (записи и само занятие), поэтому выполняется
     * в одной транзакции: либо удаляются и записи, и занятие, либо не удаляется ничего.
     */
    @Transactional
    public void delete(Long id) {
        TrainingClass trainingClass = getOrThrow(id);
        bookingRepository.deleteByTrainingClassId(id);
        classRepository.delete(trainingClass);
        classRepository.flush();
    }

    private TrainingClass getOrThrow(Long id) {
        return classRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Занятие", id));
    }

    private Trainer getTrainer(Long id) {
        return trainerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Тренер", id));
    }

    private Discipline getDiscipline(Long id) {
        return disciplineRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Направление", id));
    }
}
