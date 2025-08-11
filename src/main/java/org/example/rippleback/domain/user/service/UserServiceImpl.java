package org.example.rippleback.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.domain.user.dto.SignupRequestDto;
import org.example.rippleback.domain.user.dto.SignupResponseDto;
import org.example.rippleback.domain.user.entity.User;
import org.example.rippleback.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public SignupResponseDto signup(SignupRequestDto request) {
        User user = request.SignupToUser();

    }
}
