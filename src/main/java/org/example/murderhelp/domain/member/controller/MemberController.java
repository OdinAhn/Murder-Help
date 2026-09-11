package org.example.murderhelp.domain.member.controller;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.member.dto.MemberResponse;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(@AuthenticationPrincipal Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return ResponseEntity.ok(ApiResponse.ok(MemberResponse.from(member)));
    }
}