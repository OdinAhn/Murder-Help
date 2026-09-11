package org.example.murderhelp.domain.auth.repository;

import org.example.murderhelp.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}