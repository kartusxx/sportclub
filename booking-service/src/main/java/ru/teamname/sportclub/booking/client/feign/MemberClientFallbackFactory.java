package ru.teamname.sportclub.booking.client.feign;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Что делает {@link MemberClient}, когда member-service недоступен или цепь разомкнута. */
@Component
public class MemberClientFallbackFactory implements FallbackFactory<MemberClient> {

    private static final String SERVICE = "member-service";

    @Override
    public MemberClient create(Throwable cause) {
        return new MemberClient() {
            @Override
            public MemberView getMember(Long id) {
                throw FeignErrors.fallback(SERVICE, cause);
            }

            @Override
            public CoverageView coverage(Long id, String date) {
                throw FeignErrors.fallback(SERVICE, cause);
            }
        };
    }
}
