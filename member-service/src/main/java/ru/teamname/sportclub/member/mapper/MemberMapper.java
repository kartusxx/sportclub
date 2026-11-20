package ru.teamname.sportclub.member.mapper;

import ru.teamname.sportclub.member.dto.MemberRequest;
import ru.teamname.sportclub.member.dto.MemberResponse;
import ru.teamname.sportclub.member.entity.Member;
import ru.teamname.sportclub.member.enums.MemberStatus;

public final class MemberMapper {

    private MemberMapper() {
    }

    public static MemberResponse toResponse(Member member) {
        return new MemberResponse(member.getId(), member.getFirstName(), member.getLastName(), member.getEmail(),
                member.getPhone(), member.getBirthDate(), member.getStatus(), member.getCreatedAt());
    }

    public static Member toEntity(MemberRequest request) {
        Member member = new Member();
        apply(request, member);
        return member;
    }

    public static void apply(MemberRequest request, Member member) {
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        member.setBirthDate(request.birthDate());
        member.setStatus(request.status() != null ? request.status() : MemberStatus.ACTIVE);
    }
}
