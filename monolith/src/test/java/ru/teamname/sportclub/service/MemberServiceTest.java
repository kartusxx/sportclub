package ru.teamname.sportclub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.teamname.sportclub.dto.MemberRequest;
import ru.teamname.sportclub.dto.MemberResponse;
import ru.teamname.sportclub.entity.Member;
import ru.teamname.sportclub.enums.MemberStatus;
import ru.teamname.sportclub.exception.ConflictException;
import ru.teamname.sportclub.exception.ResourceNotFoundException;
import ru.teamname.sportclub.repository.MemberRepository;

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

    private MemberService service;
    private Member member;

    private static MemberRequest request(String email, MemberStatus status) {
        return new MemberRequest("Ivan", "Petrov", email, "+7 900 000-00-00", null, status);
    }

    @BeforeEach
    void setUp() {
        service = new MemberService(memberRepository);
        member = new Member();
        member.setId(1L);
        member.setFirstName("Ivan");
        member.setLastName("Petrov");
        member.setEmail("ivan@example.com");
        member.setStatus(MemberStatus.ACTIVE);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.saveAndFlush(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
    void deleteRemovesAndFlushes() {
        service.delete(1L);

        verify(memberRepository).delete(member);
        verify(memberRepository).flush();
    }

    @Test
    void deleteFailsForUnknownMember() {
        when(memberRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L)).isInstanceOf(ResourceNotFoundException.class);
        verify(memberRepository, never()).delete(any());
    }
}
