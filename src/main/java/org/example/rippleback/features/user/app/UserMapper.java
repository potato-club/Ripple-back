package org.example.rippleback.features.user.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.media.app.MediaUrlResolver;
import org.example.rippleback.features.user.api.dto.MeResponseDto;
import org.example.rippleback.features.user.api.dto.SignupResponseDto;
import org.example.rippleback.features.user.api.dto.UserResponseDto;
import org.example.rippleback.features.user.api.dto.UserSummaryDto;
import org.example.rippleback.features.user.domain.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final MediaUrlResolver url; // key → 공개 URL

    private String profileImageUrlOf(User u) {
        var m = u.getProfileMedia();
        if (m == null) return null;
        var key = m.getObjectKey();
        return (key == null || key.isBlank()) ? null : url.toPublicUrl(key);
    }

    public MeResponseDto toMe(User u) {
        return new MeResponseDto(
                u.getId(), u.getUsername(), u.getEmail(), u.isEmailVerified(),
                profileImageUrlOf(u),
                u.getProfileMessage(),
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
        return new UserSummaryDto(
                u.getId(),
                u.getUsername(),
                profileImageUrlOf(u),
                u.getProfileMessage()
        );
    }

    public SignupResponseDto toSignup(User u) {
        return new SignupResponseDto(u.getId(), u.getUsername(), u.isEmailVerified());
    }
}
