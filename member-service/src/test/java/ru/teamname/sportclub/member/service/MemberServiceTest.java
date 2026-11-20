package ru.teamname.sportclub.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.teamname.sportclub.member.client.BookingGateway;
import ru.teamname.sportclub.member.dto.MemberRequest;
import ru.teamname.sportclub.member.dto.MemberResponse;
import ru.teamname.sportclub.member.entity.Member;
import ru.teamname.sportclub.member.enums.MemberStatus;
import ru.teamname.sportclub.member.exception.ConflictException;
import ru.teamname.sportclub.member.exception.ResourceNotFoundException;
import ru.teamname.sportclub.member.repository.MemberRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BookingGateway bookingGateway;

    private MemberService service;
    private Member member;

    private static MemberRequest request(String email, MemberStatus status) {
        return new MemberRequest("Ivan", "Petrov", email, "+7 900 000-00-00", null, status);
    }

    @BeforeEach
    void setUp() {
        service = new MemberService(memberRepository, bookingGateway);
        member = new Member();
        member.setId(1L);
        member.setFirstName("Ivan");
        member.setLastName("Petrov");
        member.setEmail("ivan@example.com");
        member.setStatus(MemberStatus.ACTIVE);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsById(1L)).thenReturn(true);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.saveAndFlush(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void findAllMapsPageToResponses() {
        when(memberRepository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(member)));

        assertThat(service.findAll(PageRequest.of(0, 10)).getContent())
                .extracting(MemberResponse::email).containsExactly("ivan@example.com");
    }

    @Test
    void createDefaultsStatusToActive() {
        MemberResponse response = service.create(request("new@example.com", null));

        assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(response.email()).isEqualTo("new@example.com");
    }

    @Test
    void createFailsForDuplicateEmail() {
        when(memberRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("dup@example.com", MemberStatus.ACTIVE)))
                .isInstanceOf(ConflictException.class);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void updateAppliesRequest() {
        MemberResponse response = service.update(1L, request("changed@example.com", MemberStatus.FROZEN));

        assertThat(response.email()).isEqualTo("changed@example.com");
        assertThat(response.status()).isEqualTo(MemberStatus.FROZEN);
    }

    @Test
    void updateFailsWhenEmailBelongsToAnotherMember() {
        when(memberRepository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("taken@example.com", MemberStatus.ACTIVE)))
                .isInstanceOf(ConflictException.class);
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void findByIdFailsForUnknownMember() {
        when(memberRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesMemberWithoutBookings() {
        when(bookingGateway.hasBookings(1L)).thenReturn(false);

        service.delete(1L);

        verify(memberRepository).deleteById(1L);
    }

    @Test
    void deleteIsForbiddenWhenMemberHasBookings() {
        when(bookingGateway.hasBookings(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);
        verify(memberRepository, never()).deleteById(any());
    }

    @Test
    void deleteFailsForUnknownMemberWithoutAskingBookingService() {
        when(memberRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(404L)).isInstanceOf(ResourceNotFoundException.class);
        verify(bookingGateway, never()).hasBookings(any());
        verify(memberRepository, never()).deleteById(any());
    }
}
