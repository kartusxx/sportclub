package ru.teamname.sportclub.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.teamname.sportclub.member.dto.MemberRequest;
import ru.teamname.sportclub.member.dto.MemberResponse;
import ru.teamname.sportclub.member.entity.Member;
import ru.teamname.sportclub.member.exception.ConflictException;
import ru.teamname.sportclub.member.exception.ResourceNotFoundException;
import ru.teamname.sportclub.member.mapper.MemberMapper;
import ru.teamname.sportclub.member.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Page<MemberResponse> findAll(Pageable pageable) {
        return memberRepository.findAll(pageable).map(MemberMapper::toResponse);
    }

    public MemberResponse findById(Long id) {
        return MemberMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public MemberResponse create(MemberRequest request) {
        if (memberRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Участник с email " + request.email() + " уже существует");
        }
        Member saved = memberRepository.save(MemberMapper.toEntity(request));
        return MemberMapper.toResponse(saved);
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest request) {
        Member member = getOrThrow(id);
        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ConflictException("Участник с email " + request.email() + " уже существует");
        }
        MemberMapper.apply(request, member);
        return MemberMapper.toResponse(memberRepository.saveAndFlush(member));
    }

    @Transactional
    public void delete(Long id) {
        Member member = getOrThrow(id);
        memberRepository.delete(member);
        memberRepository.flush();
    }

    private Member getOrThrow(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Участник", id));
    }
}
