package ru.teamname.sportclub.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.teamname.sportclub.booking.client.ClassDirectory;
import ru.teamname.sportclub.booking.client.ClassInfo;
import ru.teamname.sportclub.booking.client.MemberDirectory;
import ru.teamname.sportclub.booking.client.MemberInfo;
import ru.teamname.sportclub.booking.dto.BookingRequest;
import ru.teamname.sportclub.booking.dto.BookingResponse;
import ru.teamname.sportclub.booking.entity.Booking;
import ru.teamname.sportclub.booking.enums.BookingStatus;
import ru.teamname.sportclub.booking.exception.BusinessRuleException;
import ru.teamname.sportclub.booking.exception.ConflictException;
import ru.teamname.sportclub.booking.exception.ResourceNotFoundException;
import ru.teamname.sportclub.booking.mapper.BookingMapper;
import ru.teamname.sportclub.booking.repository.BookingRepository;
import ru.teamname.sportclub.booking.support.ClassLock;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MemberDirectory memberDirectory;
    private final ClassDirectory classDirectory;
    private final ClassLock classLock;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<BookingResponse> findAll(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(BookingMapper::toResponse);
    }

    /** TODO(шаг 5): вернуть проверку существования занятия через catalog-service (404 для неизвестного id). */
    @Transactional(readOnly = true)
    public Page<BookingResponse> findByClass(Long classId, Pageable pageable) {
        return bookingRepository.findByClassId(classId, pageable).map(BookingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id) {
        return BookingMapper.toResponse(getOrThrow(id));
    }

    /**
     * Запись на занятие выполняется в два этапа.
     * <ol>
     *   <li>Проверки через другие сервисы (занятие, участник, абонемент) идут ДО транзакции: сетевые вызовы
     *       нельзя откатить вместе с БД, и они не должны держать блокировку или соединение с базой.</li>
     *   <li>Локальная часть выполняется в одной транзакции: консультативная блокировка занятия, проверка
     *       дубликата и свободных мест, вставка записи. Они должны быть атомарны, иначе два параллельных
     *       запроса могут занять последнее место одновременно (овербукинг).</li>
     * </ol>
     */
    public BookingResponse book(BookingRequest request) {
        ClassInfo trainingClass = classDirectory.getClassInfo(request.classId());
        MemberInfo member = memberDirectory.getMember(request.memberId());
        LocalDateTime now = LocalDateTime.now(clock);

        ensureClassNotStarted(trainingClass, now);
        ensureMemberCanBook(member, trainingClass);

        return transactionTemplate.execute(status -> saveBooking(request, trainingClass, now));
    }

    private BookingResponse saveBooking(BookingRequest request, ClassInfo trainingClass, LocalDateTime now) {
        classLock.acquire(trainingClass.id());

        Optional<Booking> existing = findReusableBooking(request.memberId(), trainingClass.id());
        ensureFreePlaces(trainingClass);

        // Отменённую запись переиспользуем: пара (участник, занятие) уникальна
        Booking booking = existing.orElseGet(Booking::new);
        booking.setMemberId(request.memberId());
        booking.setClassId(trainingClass.id());
        booking.setBookedAt(now);
        booking.setStatus(BookingStatus.BOOKED);
        booking.setNote(request.note());
        return BookingMapper.toResponse(bookingRepository.save(booking));
    }

    private void ensureClassNotStarted(ClassInfo trainingClass, LocalDateTime now) {
        if (!trainingClass.startsAt().isAfter(now)) {
            throw new BusinessRuleException("Занятие уже началось или прошло, запись невозможна");
        }
    }

    /** Участник должен быть активным и иметь абонемент, действующий на дату занятия. */
    private void ensureMemberCanBook(MemberInfo member, ClassInfo trainingClass) {
        if (!member.isActive()) {
            throw new BusinessRuleException("Запись невозможна: статус участника " + member.status());
        }
        if (!memberDirectory.hasActiveSubscription(member.id(), trainingClass.startsAt().toLocalDate())) {
            throw new BusinessRuleException("У участника нет действующего абонемента на дату занятия");
        }
    }

    /** Возвращает прежнюю запись, если она была отменена; если запись активна — это дубликат (409). */
    private Optional<Booking> findReusableBooking(Long memberId, Long classId) {
        Optional<Booking> existing = bookingRepository.findByMemberIdAndClassId(memberId, classId);
        if (existing.isPresent() && existing.get().getStatus() != BookingStatus.CANCELLED) {
            throw new ConflictException("Участник уже записан на это занятие");
        }
        return existing;
    }

    private void ensureFreePlaces(ClassInfo trainingClass) {
        long booked = bookingRepository.countByClassIdAndStatus(trainingClass.id(), BookingStatus.BOOKED);
        if (booked >= trainingClass.capacity()) {
            throw new ConflictException("На занятии нет свободных мест");
        }
    }

    public BookingResponse cancel(Long id) {
        Booking booking = getOrThrow(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessRuleException("Отменить можно только активную запись");
        }
        ClassInfo trainingClass = classDirectory.getClassInfo(booking.getClassId());
        if (!trainingClass.startsAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("Занятие уже началось, отмена невозможна");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return BookingMapper.toResponse(bookingRepository.save(booking));
    }

    public BookingResponse markAttended(Long id) {
        Booking booking = getOrThrow(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessRuleException("Отметить посещение можно только для активной записи");
        }
        ClassInfo trainingClass = classDirectory.getClassInfo(booking.getClassId());
        if (trainingClass.startsAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("Занятие ещё не началось");
        }
        booking.setStatus(BookingStatus.ATTENDED);
        return BookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public void delete(Long id) {
        bookingRepository.delete(getOrThrow(id));
    }

    private Booking getOrThrow(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Запись", id));
    }
}
