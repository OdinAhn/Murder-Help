package org.example.murderhelp.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.auth.dto.LoginRequest;
import org.example.murderhelp.domain.auth.dto.SignupRequest;
import org.example.murderhelp.domain.auth.dto.TokenResponse;
import org.example.murderhelp.domain.auth.facade.AuthFacade;
import org.example.murderhelp.domain.auth.service.AuthService;
import org.example.murderhelp.global.jwt.JwtProvider;
import org.example.murderhelp.global.response.ApiResponse;
import org.example.murderhelp.global.util.CookieUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacade authFacade;
    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtProvider jwtProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody @Valid SignupRequest request) {
        authFacade.signup(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        AuthService.TokenAndRefresh result = authService.login(request);
        cookieUtil.addRefreshTokenCookie(response, result.refreshToken(), jwtProvider.getRefreshExpiration());
        return ResponseEntity.ok(ApiResponse.ok(result.tokenResponse()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        AuthService.TokenAndRefresh result = authService.reissue(refreshToken);
        cookieUtil.addRefreshTokenCookie(response, result.refreshToken(), jwtProvider.getRefreshExpiration());
        return ResponseEntity.ok(ApiResponse.ok(result.tokenResponse()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Long memberId, HttpServletResponse response) {
        authService.logout(memberId);
        cookieUtil.deleteRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}