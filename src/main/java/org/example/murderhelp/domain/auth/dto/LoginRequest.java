package org.example.murderhelp.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "이메일을 입력하세요") String email,
        @NotBlank(message = "비밀번호를 입력하세요") String password
) {}