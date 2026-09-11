package org.example.murderhelp.global.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeMillis) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false) // 배포 시 true로 변경 (HTTPS 환경에서만 동작)
                .sameSite("Lax") // 프론트/백엔드 도메인이 다르면 "None"+secure(true) 필요
                .path("/")
                .maxAge(maxAgeMillis / 1000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}