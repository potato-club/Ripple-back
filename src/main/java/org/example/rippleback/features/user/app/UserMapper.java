package org.example.rippleback.features.user.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
import org.example.rippleback.features.media.app.MediaUrlResolver;
import org.example.rippleback.features.user.api.dto.MeResponseDto;
import org.example.rippleback.features.user.api.dto.SignupResponseDto;
import org.example.rippleback.features.user.api.dto.UserResponseDto;
import org.example.rippleback.features.user.api.dto.UserSummaryDto;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.features.user.domain.UserStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final MediaUrlResolver url;

    private static final String DELETED_USERNAME = "Ripple User";
    private static final String DEFAULT_PROFILE_KEY = "images/default-profile.png";

    // 내 정보 매핑
    public MeResponseDto toMe(User u) {
        return new MeResponseDto(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.isEmailVerified(),
                profileImageUrlOf(u),
                u.getStatus().name(),
                u.getTokenVersion(),
                u.getLastLoginAt(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }

    // 디테일한 프로필 매핑
    public UserResponseDto toProfile(User u,
                                     long postCnt,
                                     long followerCnt,
                                     long followingCnt,
                                     List<FeedResponseDto> latestFeeds
    ) {
        return new UserResponseDto(
                u.getId(),
                u.getUsername(),
                profileImageUrlOf(u),
                postCnt,
                followerCnt,
                followingCnt,
                latestFeeds
        );
    }

    // 간소화된 프로필 매핑
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

    // 회원가입 결과 매핑
    public SignupResponseDto toSignup(User u) {
        return new SignupResponseDto(u.getId(),
                u.getUsername(),
                u.isEmailVerified()
        );
    }

    // 프로필 이미지 분기 메서드
    private String profileImageUrlOf(User u) {
        // 소프트 삭제된 유저면 그냥 기본 이미지 반환
        if (u.getStatus() == UserStatus.DELETED) {
            return url.toPublicUrl(DEFAULT_PROFILE_KEY);
        }

        // 유저가 가진 media 테이블 가져옴
        var m = u.getProfileMedia();
        
        // 비어있으면 이미지 URL을 NULL로 반환
        if (m == null) return null;

        // 가져온 media 테이블에서 objectKey 꺼냄
        var key = m.getObjectKey();

        // objectKey 간단한 검증 (어차피 저장할 때 @S3ObjectKey 어노테이션으로 검증할테니까) 후 cloudfront 도메인으로 조립해서 이미지 URL 반환
        return (key == null || key.isBlank())
                ? null
                : url.toPublicUrl(key);
    }
}
