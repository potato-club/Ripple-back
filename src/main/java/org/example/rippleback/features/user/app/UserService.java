package org.example.rippleback.features.user.app;

import org.example.rippleback.features.auth.api.dto.LoginRequestDto;
import org.example.rippleback.features.user.api.dto.SignupRequestDto;
import org.example.rippleback.features.user.api.dto.SignupResponseDto;

public interface UserService {
    SignupResponseDto signup(SignupRequestDto signupRequestDto);

    SignupResponseDto changeName(Long id, String username);

    SignupResponseDto changePassword(Long id, String oldPassword, String newPassword);

    String login(LoginRequestDto loginRequestDto);

    void logout(Long id);

    void deleteInfo(Long id);
}