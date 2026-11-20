package ru.teamname.sportclub.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionCallback;
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
import ru.teamname.sportclub.booking.exception.ServiceUnavailableException;
import ru.teamname.sportclub.booking.repository.BookingRepository;
import ru.teamname.sportclub.booking.support.ClassLock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 10, 0);

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private MemberDirectory memberDirectory;
    @Mock
    private ClassDirectory classDirectory;
    @Mock
    private ClassLock classLock;
    @Mock
    private TransactionTemplate transactionTemplate;

    private BookingService service;
    private ClassInfo trainingClass;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new BookingService(bookingRepository, memberDirectory, classDirectory, classLock,
                transactionTemplate, CLOCK);

        trainingClass = new ClassInfo(10L, "Yoga", NOW.plusDays(1), 2);
        when(classDirectory.getClassInfo(10L)).thenReturn(trainingClass);
        when(memberDirectory.getMember(1L)).thenReturn(new MemberInfo(1L, "Ivan Petrov", "ACTIVE"));
        when(memberDirectory.hasActiveSubscription(1L, NOW.plusDays(1).toLocalDate())).thenReturn(true);
        when(bookingRepository.findByMemberIdAndClassId(1L, 10L)).thenReturn(Optional.empty());
        when(bookingRepository.countByClassIdAndStatus(10L, BookingStatus.BOOKED)).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(call -> {
            Booking saved = call.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(100L);
            }
            return saved;
        });
        // Транзакция в юнит-тесте: просто выполняем код внутри TransactionTemplate
        when(transactionTemplate.execute(any())).thenAnswer(call ->
                ((TransactionCallback<Object>) call.getArgument(0)).doInTransaction(null));
    }

    private Booking existingBooking(BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(5L);
        booking.setMemberId(1L);
        booking.setClassId(10L);
        booking.setStatus(status);
        booking.setBookedAt(NOW.minusDays(1));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        return booking;
    }

    private void classStartsAt(LocalDateTime startsAt) {
        trainingClass = new ClassInfo(10L, "Yoga", startsAt, 2);
        when(classDirectory.getClassInfo(10L)).thenReturn(trainingClass);
    }

    // ---- запись ----

    @Test
    void bookCreatesBookedBooking() {
        BookingResponse response = service.book(new BookingRequest(1L, 10L, "note"));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(BookingStatus.BOOKED);
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.classId()).isEqualTo(10L);
        assertThat(response.bookedAt()).isEqualTo(NOW);
        assertThat(response.note()).isEqualTo("note");
    }

    @Test
    void bookLocksClassBeforeCheckingFreePlaces() {
        service.book(new BookingRequest(1L, 10L, null));

        InOrder order = inOrder(classLock, bookingRepository);
        order.verify(classLock).acquire(10L);
        order.verify(bookingRepository).countByClassIdAndStatus(10L, BookingStatus.BOOKED);
        order.verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void bookFailsWhenClassNotFound() {
        when(classDirectory.getClassInfo(99L)).thenThrow(new ResourceNotFoundException("Занятие", 99L));

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 99L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(transactionTemplate);
    }

    @Test
    void bookFailsWhenMemberNotFound() {
        when(memberDirectory.getMember(99L)).thenThrow(new ResourceNotFoundException("Участник", 99L));

        assertThatThrownBy(() -> service.book(new BookingRequest(99L, 10L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bookFailsWhenClassAlreadyStarted() {
        classStartsAt(NOW.minusMinutes(1));

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookFailsWhenMemberIsNotActive() {
        when(memberDirectory.getMember(1L)).thenReturn(new MemberInfo(1L, "Ivan Petrov", "BLOCKED"));

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BLOCKED");
    }

    @Test
    void bookFailsWithoutActiveSubscription() {
        when(memberDirectory.hasActiveSubscription(1L, NOW.plusDays(1).toLocalDate())).thenReturn(false);

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void networkChecksHappenBeforeTheTransactionStarts() {
        when(memberDirectory.hasActiveSubscription(1L, NOW.plusDays(1).toLocalDate())).thenReturn(false);

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(BusinessRuleException.class);

        // ни транзакция, ни блокировка занятия не открывались
        verifyNoInteractions(transactionTemplate, classLock);
    }

    @Test
    void bookPropagatesUnavailabilityOfAnotherService() {
        when(memberDirectory.getMember(1L)).thenThrow(new ServiceUnavailableException("member-service недоступен"));

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(ServiceUnavailableException.class);
        verifyNoInteractions(transactionTemplate);
    }

    @Test
    void bookFailsWhenAlreadyBooked() {
        Booking booked = existingBooking(BookingStatus.BOOKED);
        when(bookingRepository.findByMemberIdAndClassId(1L, 10L)).thenReturn(Optional.of(booked));

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void bookFailsWhenClassIsFull() {
        when(bookingRepository.countByClassIdAndStatus(10L, BookingStatus.BOOKED)).thenReturn(2L);

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(ConflictException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookReusesCancelledBooking() {
        Booking cancelled = existingBooking(BookingStatus.CANCELLED);
        when(bookingRepository.findByMemberIdAndClassId(1L, 10L)).thenReturn(Optional.of(cancelled));

        service.book(new BookingRequest(1L, 10L, "again"));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(cancelled);
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.BOOKED);
        assertThat(cancelled.getBookedAt()).isEqualTo(NOW);
        assertThat(cancelled.getNote()).isEqualTo("again");
    }

    // ---- отмена и посещение ----

    @Test
    void cancelMarksBookingCancelled() {
        existingBooking(BookingStatus.BOOKED);

        assertThat(service.cancel(5L).status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelFailsForNonBookedBooking() {
        existingBooking(BookingStatus.CANCELLED);

        assertThatThrownBy(() -> service.cancel(5L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancelFailsAfterClassStarted() {
        existingBooking(BookingStatus.BOOKED);
        classStartsAt(NOW.minusHours(1));

        assertThatThrownBy(() -> service.cancel(5L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancelFailsWhenBookingNotFound() {
        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(404L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAttendedWorksAfterClassStarted() {
        existingBooking(BookingStatus.BOOKED);
        classStartsAt(NOW.minusHours(1));

        assertThat(service.markAttended(5L).status()).isEqualTo(BookingStatus.ATTENDED);
    }

    @Test
    void markAttendedFailsBeforeClassStarts() {
        existingBooking(BookingStatus.BOOKED);

        assertThatThrownBy(() -> service.markAttended(5L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markAttendedFailsForCancelledBooking() {
        existingBooking(BookingStatus.CANCELLED);
        classStartsAt(NOW.minusHours(1));

        assertThatThrownBy(() -> service.markAttended(5L)).isInstanceOf(BusinessRuleException.class);
    }

    // ---- чтение и удаление ----

    @Test
    void findByIdReturnsBooking() {
        existingBooking(BookingStatus.BOOKED);

        assertThat(service.findById(5L).id()).isEqualTo(5L);
    }

    @Test
    void findByIdFailsForUnknownBooking() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void findAllMapsPageToResponses() {
        Booking booking = existingBooking(BookingStatus.BOOKED);
        when(bookingRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(booking)));

        Page<BookingResponse> page = service.findAll(Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(5L);
    }

    @Test
    void findByClassChecksThatClassExists() {
        when(classDirectory.getClassInfo(99L)).thenThrow(new ResourceNotFoundException("Занятие", 99L));

        assertThatThrownBy(() -> service.findByClass(99L, Pageable.unpaged()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bookingRepository, never()).findByClassId(anyLong(), any());
    }

    @Test
    void findByClassReadsBookingsOfExistingClass() {
        Booking booking = existingBooking(BookingStatus.BOOKED);
        when(bookingRepository.findByClassId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        assertThat(service.findByClass(10L, Pageable.unpaged()).getContent()).hasSize(1);
    }

    @Test
    void deleteRemovesBooking() {
        Booking booking = existingBooking(BookingStatus.BOOKED);

        service.delete(5L);

        verify(bookingRepository).delete(booking);
    }

    @Test
    void deleteFailsForUnknownBooking() {
        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- внутренние операции для других сервисов ----

    @Test
    void countBookedCountsOnlyActiveBookings() {
        when(bookingRepository.countByClassIdAndStatus(10L, BookingStatus.BOOKED)).thenReturn(7L);

        assertThat(service.countBooked(10L)).isEqualTo(7L);
    }

    @Test
    void hasBookingsDelegatesToRepository() {
        when(bookingRepository.existsByMemberId(1L)).thenReturn(true);
        when(bookingRepository.existsByMemberId(2L)).thenReturn(false);

        assertThat(service.hasBookings(1L)).isTrue();
        assertThat(service.hasBookings(2L)).isFalse();
    }

    @Test
    void deleteByClassRemovesAllBookingsOfClass() {
        service.deleteByClass(10L);

        verify(bookingRepository).deleteAllByClassId(10L);
    }
}
