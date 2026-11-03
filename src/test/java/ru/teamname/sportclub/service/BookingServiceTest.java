package ru.teamname.sportclub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import ru.teamname.sportclub.repository.BookingRepository;
import ru.teamname.sportclub.repository.MemberRepository;
import ru.teamname.sportclub.repository.SubscriptionRepository;
import ru.teamname.sportclub.repository.TrainingClassRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 10, 0);

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private TrainingClassRepository classRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private BookingService service;
    private Member member;
    private TrainingClass trainingClass;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookingRepository, memberRepository, classRepository,
                subscriptionRepository, CLOCK);

        member = new Member();
        member.setId(1L);
        member.setFirstName("Ivan");
        member.setLastName("Petrov");
        member.setStatus(MemberStatus.ACTIVE);

        trainingClass = new TrainingClass();
        trainingClass.setId(10L);
        trainingClass.setTitle("Yoga");
        trainingClass.setCapacity(2);
        trainingClass.setStartsAt(NOW.plusDays(1));

        when(classRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trainingClass));
        when(classRepository.existsById(10L)).thenReturn(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(subscriptionRepository.countCovering(1L, SubscriptionStatus.ACTIVE, NOW.plusDays(1).toLocalDate()))
                .thenReturn(1L);
        when(bookingRepository.findByMemberIdAndTrainingClassId(1L, 10L)).thenReturn(Optional.empty());
        when(bookingRepository.countByTrainingClassIdAndStatus(10L, BookingStatus.BOOKED)).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Booking existingBooking(BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(5L);
        booking.setMember(member);
        booking.setTrainingClass(trainingClass);
        booking.setStatus(status);
        booking.setBookedAt(NOW.minusDays(1));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        return booking;
    }

    @Test
    void bookCreatesBookedBooking() {
        BookingResponse response = service.book(new BookingRequest(1L, 10L, "note"));

        assertThat(response.status()).isEqualTo(BookingStatus.BOOKED);
        assertThat(response.memberName()).isEqualTo("Ivan Petrov");
        assertThat(response.classTitle()).isEqualTo("Yoga");
        assertThat(response.bookedAt()).isEqualTo(NOW);
        assertThat(response.note()).isEqualTo("note");
    }

    @Test
    void bookFailsWhenClassNotFound() {
        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 99L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bookFailsWhenMemberNotFound() {
        assertThatThrownBy(() -> service.book(new BookingRequest(99L, 10L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bookFailsWhenClassAlreadyStarted() {
        trainingClass.setStartsAt(NOW.minusMinutes(1));

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookFailsWhenMemberIsNotActive() {
        member.setStatus(MemberStatus.BLOCKED);

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void bookFailsWithoutActiveSubscription() {
        when(subscriptionRepository.countCovering(1L, SubscriptionStatus.ACTIVE, NOW.plusDays(1).toLocalDate()))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookFailsWhenAlreadyBooked() {
        Booking booked = existingBooking(BookingStatus.BOOKED);
        when(bookingRepository.findByMemberIdAndTrainingClassId(1L, 10L)).thenReturn(Optional.of(booked));

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void bookFailsWhenClassIsFull() {
        when(bookingRepository.countByTrainingClassIdAndStatus(10L, BookingStatus.BOOKED)).thenReturn(2L);

        assertThatThrownBy(() -> service.book(new BookingRequest(1L, 10L, null)))
                .isInstanceOf(ConflictException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookReusesCancelledBooking() {
        Booking cancelled = existingBooking(BookingStatus.CANCELLED);
        when(bookingRepository.findByMemberIdAndTrainingClassId(1L, 10L)).thenReturn(Optional.of(cancelled));

        service.book(new BookingRequest(1L, 10L, "again"));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(cancelled);
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.BOOKED);
        assertThat(cancelled.getBookedAt()).isEqualTo(NOW);
        assertThat(cancelled.getNote()).isEqualTo("again");
    }

    @Test
    void cancelMarksBookingCancelled() {
        existingBooking(BookingStatus.BOOKED);

        BookingResponse response = service.cancel(5L);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelFailsForNonBookedBooking() {
        existingBooking(BookingStatus.CANCELLED);

        assertThatThrownBy(() -> service.cancel(5L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancelFailsAfterClassStarted() {
        existingBooking(BookingStatus.BOOKED);
        trainingClass.setStartsAt(NOW.minusHours(1));

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
        trainingClass.setStartsAt(NOW.minusHours(1));

        BookingResponse response = service.markAttended(5L);

        assertThat(response.status()).isEqualTo(BookingStatus.ATTENDED);
    }

    @Test
    void markAttendedFailsBeforeClassStarts() {
        existingBooking(BookingStatus.BOOKED);

        assertThatThrownBy(() -> service.markAttended(5L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markAttendedFailsForCancelledBooking() {
        existingBooking(BookingStatus.CANCELLED);
        trainingClass.setStartsAt(NOW.minusHours(1));

        assertThatThrownBy(() -> service.markAttended(5L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deleteRemovesBooking() {
        Booking booking = existingBooking(BookingStatus.BOOKED);

        service.delete(5L);

        verify(bookingRepository).delete(booking);
    }

    @Test
    void findByClassFailsForUnknownClass() {
        when(classRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByClass(99L, org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
