package org.example.rippleback.features.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequestDto(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "영문/숫자/밑줄 3~20자")
        String username,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,

        @NotBlank(message = "디바이스 ID가 팔요합니다.")
        String deviceId
) {
}