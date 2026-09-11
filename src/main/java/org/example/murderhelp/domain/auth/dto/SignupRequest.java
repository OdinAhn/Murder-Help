package org.example.murderhelp.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(
                regexp = "^010-\\d{4}-\\d{4}$",
                message = "전화번호는 010-XXXX-XXXX 형식이어야 합니다.")
        String phone
) {
}
