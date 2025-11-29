package org.example.rippleback.features.user.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.media.app.MediaUrlResolver;
import org.example.rippleback.features.user.api.dto.MeResponseDto;
import org.example.rippleback.features.user.api.dto.SignupResponseDto;
import org.example.rippleback.features.user.api.dto.UserResponseDto;
import org.example.rippleback.features.user.api.dto.UserSummaryDto;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.features.user.domain.UserStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final MediaUrlResolver url;

    private static final String DELETED_USERNAME = "Ripple User";
    private static final String DEFAULT_PROFILE_KEY = "images/default-profile.png";

    private String profileImageUrlOf(User u) {
        if (u.getStatus() == UserStatus.DELETED) {
            return url.toPublicUrl(DEFAULT_PROFILE_KEY);
        }

        var m = u.getProfileMedia();
        if (m == null) return null;
        var key = m.getObjectKey();
        return (key == null || key.isBlank())
                ? null
                : url.toPublicUrl(key);
    }

    public MeResponseDto toMe(User u) {
        return new MeResponseDto(
                u.getId(), u.getUsername(), u.getEmail(), u.isEmailVerified(),
                profileImageUrlOf(u),
                u.getStatus().name(), u.getTokenVersion(),
                u.getLastLoginAt(), u.getCreatedAt(), u.getUpdatedAt()
        );
    }

    public UserResponseDto toProfile(User u, long postCnt, long followerCnt, long followingCnt) {
        return new UserResponseDto(
                u.getId(),
                u.getUsername(),
                profileImageUrlOf(u),
                postCnt,
                followerCnt,
                followingCnt
        );
    }

    public UserSummaryDto toSummary(User u) {
        String username = u.getUsername();
        if (u.getStatus() == UserStatus.DELETED) {
            username = DELETED_USERNAME;
        }

        return new UserSummaryDto(
                u.getId(),
                username,
                profileImageUrlOf(u)
        );
    }

    public SignupResponseDto toSignup(User u) {
        return new SignupResponseDto(u.getId(), u.getUsername(), u.isEmailVerified());
    }
}
