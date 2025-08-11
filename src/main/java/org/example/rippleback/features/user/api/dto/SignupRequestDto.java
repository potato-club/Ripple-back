package org.example.rippleback.features.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.rippleback.features.user.domain.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class SignupRequestDto {

    private String username;
    private String password;
    private String email;

    public User SignupToUser() {
        return User.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();
    }
}
