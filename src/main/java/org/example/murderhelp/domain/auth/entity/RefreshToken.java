package org.example.murderhelp.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.global.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_member", columnNames = "member_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 500)
    private String token;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    private  RefreshToken(Long memberId, String token, LocalDateTime expiredAt) {
        this.memberId = memberId;
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public void update(String token, LocalDateTime expiredAt) {
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public boolean isExpired() {
        return this.expiredAt.isBefore(LocalDateTime.now());
    }

    public boolean matches(String token) {
        return this.token.equals(token);
    }
}