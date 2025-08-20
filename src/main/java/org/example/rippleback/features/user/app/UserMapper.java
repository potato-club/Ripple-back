package org.example.rippleback.features.user.app;

import org.example.rippleback.features.user.api.dto.MeResponseDto;
import org.example.rippleback.features.user.api.dto.SignupResponseDto;
import org.example.rippleback.features.user.api.dto.UserResponseDto;
import org.example.rippleback.features.user.api.dto.UserSummaryDto;
import org.example.rippleback.features.user.domain.User;

public final class UserMapper {
    private UserMapper() {
    }

    public static MeResponseDto toMe(User u) {
        return new MeResponseDto(
                u.getId(), u.getUsername(), u.getEmail(), u.isEmailVerified(),
                u.getProfileImageUrl(), u.getProfileMessage(),
                u.getStatus().name(), u.getTokenVersion(),
                u.getLastLoginAt(), u.getCreatedAt(), u.getUpdatedAt()
        );
    }

    public static UserResponseDto toProfile(User u) {
        return new UserResponseDto(
                u.getId(), u.getUsername(), u.getProfileImageUrl(), u.getProfileMessage(), u.getStatus().name()
        );
    }

    public static UserSummaryDto toSummary(User u) {
        return new UserSummaryDto(
                u.getId(), u.getUsername(), u.getProfileImageUrl(), u.getProfileMessage()
        );
    }

    public static SignupResponseDto toSignup(User u) {
        return new SignupResponseDto(u.getId(), u.getUsername(), u.isEmailVerified());
    }
}
