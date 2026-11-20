package ru.teamname.sportclub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teamname.sportclub.dto.BookingRequest;
import ru.teamname.sportclub.dto.BookingResponse;
import ru.teamname.sportclub.entity.Booking;
import ru.teamname.sportclub.entity.Member;
import ru.teamname.sportclub.entity.TrainingClass;
import ru.teamname.sportclub.enums.BookingStatus;
import ru.teamname.sportclub.enums.MemberStatus;
import ru.teamname.sportclub.enums.SubscriptionStatus;
import ru.teamname.sportclub.exception.BusinessRuleException;
import ru.teamname.sportclub.exception.ConflictException;
import ru.teamname.sportclub.exception.ResourceNotFoundException;
import ru.teamname.sportclub.mapper.BookingMapper;
import ru.teamname.sportclub.repository.BookingRepository;
import ru.teamname.sportclub.repository.MemberRepository;
import ru.teamname.sportclub.repository.SubscriptionRepository;
import ru.teamname.sportclub.repository.TrainingClassRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MemberRepository memberRepository;
    private final TrainingClassRepository classRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final Clock clock;

    public Page<BookingResponse> findAll(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(BookingMapper::toResponse);
    }

    public Page<BookingResponse> findByClass(Long classId, Pageable pageable) {
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Занятие", classId);
        }
        return bookingRepository.findByTrainingClassId(classId, pageable).map(BookingMapper::toResponse);
    }

    public BookingResponse findById(Long id) {
        return BookingMapper.toResponse(getOrThrow(id));
    }

    /**
     * Запись на занятие выполняется в одной транзакции с блокировкой строки занятия:
     * проверки (абонемент, дубликат, свободные места) и вставка записи должны быть атомарны,
     * иначе два параллельных запроса могут занять последнее место одновременно (овербукинг).
     */
    @Transactional
    public BookingResponse book(BookingRequest request) {
        TrainingClass trainingClass = classRepository.findLockedById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Занятие", request.classId()));
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Участник", request.memberId()));
        LocalDateTime now = LocalDateTime.now(clock);

        ensureClassNotStarted(trainingClass, now);
        ensureMemberCanBook(member, trainingClass);
        Optional<Booking> existing = findReusableBooking(member, trainingClass);
        ensureFreePlaces(trainingClass);

        // Отменённую запись переиспользуем: пара (участник, занятие) уникальна
        Booking booking = existing.orElseGet(Booking::new);
        booking.setMember(member);
        booking.setTrainingClass(trainingClass);
        booking.setBookedAt(now);
        booking.setStatus(BookingStatus.BOOKED);
        booking.setNote(request.note());
        return BookingMapper.toResponse(bookingRepository.save(booking));
    }

    private void ensureClassNotStarted(TrainingClass trainingClass, LocalDateTime now) {
        if (!trainingClass.getStartsAt().isAfter(now)) {
            throw new BusinessRuleException("Занятие уже началось или прошло, запись невозможна");
        }
    }

    /** Участник должен быть активным и иметь абонемент, действующий на дату занятия. */
    private void ensureMemberCanBook(Member member, TrainingClass trainingClass) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException("Запись невозможна: статус участника " + member.getStatus());
        }
        LocalDate classDate = trainingClass.getStartsAt().toLocalDate();
        boolean hasCoveringSubscription = subscriptionRepository
                .existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        member.getId(), SubscriptionStatus.ACTIVE, classDate, classDate);
        if (!hasCoveringSubscription) {
            throw new BusinessRuleException("У участника нет действующего абонемента на дату занятия");
        }
    }

    /** Возвращает прежнюю запись, если она была отменена; если запись активна — это дубликат (409). */
    private Optional<Booking> findReusableBooking(Member member, TrainingClass trainingClass) {
        Optional<Booking> existing = bookingRepository
                .findByMemberIdAndTrainingClassId(member.getId(), trainingClass.getId());
        if (existing.isPresent() && existing.get().getStatus() != BookingStatus.CANCELLED) {
            throw new ConflictException("Участник уже записан на это занятие");
        }
        return existing;
    }

    private void ensureFreePlaces(TrainingClass trainingClass) {
        long booked = bookingRepository.countByTrainingClassIdAndStatus(trainingClass.getId(), BookingStatus.BOOKED);
        if (booked >= trainingClass.getCapacity()) {
            throw new ConflictException("На занятии нет свободных мест");
        }
    }

    @Transactional
    public BookingResponse cancel(Long id) {
        Booking booking = getOrThrow(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessRuleException("Отменить можно только активную запись");
        }
        if (!booking.getTrainingClass().getStartsAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("Занятие уже началось, отмена невозможна");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return BookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse markAttended(Long id) {
        Booking booking = getOrThrow(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessRuleException("Отметить посещение можно только для активной записи");
        }
        if (booking.getTrainingClass().getStartsAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("Занятие ещё не началось");
        }
        booking.setStatus(BookingStatus.ATTENDED);
        return BookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public void delete(Long id) {
        Booking booking = getOrThrow(id);
        bookingRepository.delete(booking);
    }

    private Booking getOrThrow(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Запись", id));
    }
}
