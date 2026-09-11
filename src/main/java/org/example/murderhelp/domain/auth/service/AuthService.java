package org.example.murderhelp.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.auth.dto.LoginRequest;
import org.example.murderhelp.domain.auth.dto.TokenResponse;
import org.example.murderhelp.domain.auth.entity.RefreshToken;
import org.example.murderhelp.domain.auth.repository.RefreshTokenRepository;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.example.murderhelp.global.jwt.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public TokenAndRefresh login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.createAccessToken(member.getId(), member.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(member.getId());

        saveOrUpdateRefreshToken(member.getId(), refreshToken);

        return new TokenAndRefresh(new TokenResponse(accessToken), refreshToken);
    }

    @Transactional
    public TokenAndRefresh reissue(String refreshToken) {
        if (refreshToken == null || !jwtProvider.validate(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = jwtProvider.getMemberId(refreshToken);

        RefreshToken saved = refreshTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (saved.isExpired() || !saved.matches(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(member.getId(), member.getEmail());
        String newRefreshToken = jwtProvider.createRefreshToken(member.getId());

        saved.update(newRefreshToken, LocalDateTime.now().plusSeconds(jwtProvider.getRefreshExpiration() / 1000));

        return new TokenAndRefresh(new TokenResponse(newAccessToken), newRefreshToken);
    }

    @Transactional
    public void logout(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    private void saveOrUpdateRefreshToken(Long memberId, String refreshToken) {
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshExpiration() / 1000);

        refreshTokenRepository.findByMemberId(memberId)
                .ifPresentOrElse(
                        existing -> existing.update(refreshToken, expiredAt),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .memberId(memberId)
                                .token(refreshToken)
                                .expiredAt(expiredAt)
                                .build())
                );
    }

    public record TokenAndRefresh(TokenResponse tokenResponse, String refreshToken) {
    }
}