package ru.teamname.sportclub.booking.client.feign;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.teamname.sportclub.booking.client.MemberDirectory;
import ru.teamname.sportclub.booking.client.MemberInfo;
import ru.teamname.sportclub.booking.exception.ResourceNotFoundException;

import java.time.LocalDate;

/** Реализация порта {@link MemberDirectory} поверх Feign-клиента. */
@Component
@RequiredArgsConstructor
public class FeignMemberDirectory implements MemberDirectory {

    private static final String SERVICE = "member-service";

    private final MemberClient memberClient;

    @Override
    public MemberInfo getMember(Long memberId) {
        try {
            MemberView view = memberClient.getMember(memberId);
            return new MemberInfo(view.id(), view.fullName(), view.status());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Участник", memberId);
        } catch (FeignException e) {
            throw FeignErrors.unavailable(SERVICE, e);
        }
    }

    @Override
    public boolean hasActiveSubscription(Long memberId, LocalDate date) {
        try {
            return memberClient.coverage(memberId, date.toString()).covered();
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Участник", memberId);
        } catch (FeignException e) {
            throw FeignErrors.unavailable(SERVICE, e);
        }
    }
}
