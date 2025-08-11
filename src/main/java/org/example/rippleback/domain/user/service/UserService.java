package org.example.rippleback.domain.user.service;

import org.example.rippleback.domain.auth.dto.LoginRequestDto;
import org.example.rippleback.domain.user.dto.SignupRequestDto;
import org.example.rippleback.domain.user.dto.SignupResponseDto;

public interface UserService {
    SignupResponseDto signup(SignupRequestDto signupRequestDto);

    SignupResponseDto changeName(Long id, String username);

    SignupResponseDto changePassword(Long id, String oldPassword, String newPassword);

    String login(LoginRequestDto loginRequestDto);

    void logout(Long id);

    void deleteInfo(Long id);
}