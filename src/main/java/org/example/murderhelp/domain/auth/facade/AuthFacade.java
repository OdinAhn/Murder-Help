package org.example.murderhelp.domain.auth.facade;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.auth.dto.SignupRequest;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.entity.MemberSpending;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.domain.member.repository.MemberSpendingRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuthFacade {

    private final MemberRepository memberRepository;
    private final MemberSpendingRepository memberSpendingRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .build();
        memberRepository.save(member);

        memberSpendingRepository.save(MemberSpending.builder()
                .memberId(member.getId())
                .build());
    }
}