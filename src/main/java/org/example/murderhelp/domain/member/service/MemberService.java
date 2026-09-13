package org.example.murderhelp.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional
    public Member getSystemBotMember() {
        return memberRepository.findByEmail("bot@system.com")
                .orElseGet(() -> {
                    Member bot = Member.builder()
                            .email("bot@system.com")
                            .name("시스템 봇")
                            .password("NONE")
                            .phone("000-0000-0000")
                            .build();
                    bot.changeGrade(org.example.murderhelp.domain.member.entity.Grade.GREEN);
                    return memberRepository.save(bot);
                });
    }
}
