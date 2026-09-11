package org.example.murderhelp.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;
    private final JwtParser parser;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.parser = Jwts.parser()
                .verifyWith(this.key)
                .build();
    }

    public String createAccessToken(Long memberId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(memberId.toString())
                .claim("email", email)
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiration))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(memberId.toString())
                .claim("type", "REFRESH")
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(key)
                .compact();
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(parseClaims(token).get("type", String.class));
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return parser
                .parseSignedClaims(token)
                .getPayload();
    }
}