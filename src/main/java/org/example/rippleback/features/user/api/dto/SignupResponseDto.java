package org.example.rippleback.features.user.api.dto;

import lombok.Builder;
import org.example.rippleback.features.user.domain.User;

@Builder

public class SignupResponseDto {

    private String username;
    private String email;
    private String password;

    public static SignupResponseDto SignupToUserDto(User user) {
        return SignupResponseDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .build();
    }
}
