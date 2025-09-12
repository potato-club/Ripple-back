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
        // lazy 연관: u.getProfileMedia()는 트랜잭션 안에서 접근 (지금 서비스 메서드들이 @Transactional 이므로 OK)
        var m = u.getProfileMedia();
        if (m == null) return null;
        var key = m.getObjectKey();
        return (key == null || key.isBlank()) ? null : url.toPublicUrl(key);
    }

    public MeResponseDto toMe(User u) {
        return new MeResponseDto(
                u.getId(), u.getUsername(), u.getEmail(), u.isEmailVerified(),
                profileImageUrlOf(u),                      // ← 여기서만 URL 조립
                u.getProfileMessage(),
                u.getStatus().name(), u.getTokenVersion(),
                u.getLastLoginAt(), u.getCreatedAt(), u.getUpdatedAt()
        );
    }

    public UserResponseDto toProfile(User u) {
        return new UserResponseDto(
                u.getId(),
                u.getUsername(),
                profileImageUrlOf(u),                      // ← URL
                u.getProfileMessage(),
                u.getStatus().name()
        );
    }

    public UserSummaryDto toSummary(User u) {
        return new UserSummaryDto(
                u.getId(),
                u.getUsername(),
                profileImageUrlOf(u),                      // ← URL
                u.getProfileMessage()
        );
    }

    public SignupResponseDto toSignup(User u) {
        return new SignupResponseDto(u.getId(), u.getUsername(), u.isEmailVerified());
    }
}
