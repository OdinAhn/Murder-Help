package org.example.murderhelp.domain.member.dto;

import org.example.murderhelp.domain.member.entity.Member;

public record MemberResponse(
        Long id,
        String email,
        String name,
        String phone,
        String grade,
        String profileImageUrl
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPhone(),
                member.getGrade().name(),
                member.getProfileImageUrl()
        );
    }
}