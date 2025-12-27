package org.example.rippleback.features.user.app;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import org.example.rippleback.features.feed.app.FeedService;
import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedStatus;
import org.example.rippleback.features.feed.infra.FeedBookmarkRepository;
import org.example.rippleback.features.feed.infra.FeedLikeRepository;
import org.example.rippleback.features.feed.infra.FeedRepository;
import org.example.rippleback.features.media.domain.Media;
import org.example.rippleback.features.media.infra.MediaRepository;
import org.example.rippleback.features.user.api.dto.*;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.features.user.domain.UserBlock;
import org.example.rippleback.features.user.domain.UserFollow;
import org.example.rippleback.features.user.domain.UserStatus;
import org.example.rippleback.features.user.infra.UserBlockRepository;
import org.example.rippleback.features.user.infra.UserFollowRepository;
import org.example.rippleback.features.user.infra.UserRepository;
import org.example.rippleback.infra.redis.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepo;
    private final UserBlockRepository userBlockRepo;
    private final FeedRepository feedRepo;
    private final MediaRepository mediaRepo;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final Clock clock;
    private final FeedService feedService;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedBookmarkRepository feedBookmarkRepository;
    private final S3Presigner s3Presigner;

    @Value("${media.bucket}")
    private String mediaBucket;
    private static final long MAX_PROFILE_IMAGE_BYTES = 5 * 1024 * 1024L;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public SignupResponseDto signup(SignupRequestDto req) {
        String normalizedEmail = req.email().trim().toLowerCase(Locale.ROOT);

        if (!emailVerificationService.isVerified(normalizedEmail)) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(req.username())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        User u = User.builder()
                .username(req.username())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(req.password()))
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .build();
        userRepository.save(u);
        emailVerificationService.clear(normalizedEmail);
        return userMapper.toSignup(u);
    }

    @Transactional(readOnly = true)
    public MeResponseDto getMe(Long meId) {
        User u = userRepository.findById(meId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toMe(u);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getProfileById(Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (u.getStatus() != UserStatus.ACTIVE) throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        long posts = feedRepo.countByAuthorIdAndStatus(id, FeedStatus.PUBLISHED);
        long followers = userFollowRepo.countByFollowingId(id);
        long followings = userFollowRepo.countByFollowerId(id);

        return userMapper.toProfile(u, posts, followers, followings);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getProfileByUsername(String username) {
        User u = userRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (u.getStatus() != UserStatus.ACTIVE) throw new BusinessException(ErrorCode.USER_NOT_FOUND, "비활성화된 계정입니다.");

        long id = u.getId();
        long posts = feedRepo.countByAuthorIdAndStatus(id, FeedStatus.PUBLISHED);
        long followers = userFollowRepo.countByFollowingId(id);
        long followings = userFollowRepo.countByFollowerId(id);

        return userMapper.toProfile(u, posts, followers, followings);
    }

    @Transactional(readOnly = true)
    public PageCursorResponse<UserSummaryDto> search(String q, Long cursor, int size) {
        int pageSize = Math.max(1, Math.min(size, 50));
        List<User> users = userRepository.search(q == null ? "" : q, cursor, PageRequest.of(0, pageSize));
        String next = users.size() == pageSize ? String.valueOf(users.get(users.size() - 1).getId()) : null;
        boolean hasNext = next != null;
        List<UserSummaryDto> items = users.stream().map(userMapper::toSummary).toList();
        return new PageCursorResponse<>(items, next, hasNext);
    }

    @Transactional(readOnly = true)
    public ProfileImagePresignResponseDto prepareProfileImageUpload(Long meId, ProfileImagePresignRequestDto req) {
        User me = loadActiveForWrite(meId);

        if (req.sizeBytes() != null && req.sizeBytes() > MAX_PROFILE_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.MEDIA_SIZE_EXCEEDED);
        }

        String ext = mapImageExtFromMime(req.mimeType());
        if (ext == null) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        String objectKey = buildProfileImageKey(me.getId(), ext);

        String uploadUrl = createPresignedPutUrl(objectKey, req.mimeType(), req.sizeBytes());

        return new ProfileImagePresignResponseDto(
                uploadUrl,
                objectKey,
                MAX_PROFILE_IMAGE_BYTES
        );
    }

    @Transactional
    public UserResponseDto updateProfile(Long meId, UpdateProfileRequestDto req) {
        User me = loadActiveForWrite(meId);

        if (req.username() != null) {
            String nu = req.username().strip();
            if (!nu.equalsIgnoreCase(me.getUsername())) {
                if (userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(nu)) {
                    throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
                }
                me.changeUsername(nu);
            }
        }

        var img = req.profileImage();
        var action = (img == null || img.action() == null)
                ? UpdateProfileRequestDto.ProfileImagePatch.Action.KEEP
                : img.action();

        switch (action) {
            case KEEP -> {
            }
            case CLEAR -> {
                me.clearProfileImage();
            }
            case SET -> {
                if (img.objectKey() == null || img.objectKey().isBlank()) {
                    throw new BusinessException(ErrorCode.INVALID_OBJECT_KEY);
                }

                String expectedPrefix = "users/" + meId + "/";
                if (!img.objectKey().startsWith(expectedPrefix)) {
                    throw new BusinessException(ErrorCode.INVALID_OBJECT_KEY);
                }

                Media media = Media.newProfileImage(
                        me.getId(),
                        img.objectKey(),
                        img.mimeType(),
                        img.width(),
                        img.height(),
                        img.sizeBytes()
                );
                mediaRepo.save(media);

                me.updateProfileImage(media.getId());

                em.flush();
                em.refresh(me);
            }
        }

        long posts = feedRepo.countByAuthorIdAndStatus(meId, FeedStatus.PUBLISHED);
        long followers = userFollowRepo.countByFollowingId(meId);
        long followings = userFollowRepo.countByFollowerId(meId);

        return userMapper.toProfile(me, posts, followers, followings);
    }


    @Transactional
    public void changePassword(Long meId, String current, String newPw) {
        User me = loadActiveForWrite(meId);
        if (!passwordEncoder.matches(current, me.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        me.changePassword(passwordEncoder.encode(newPw));
    }

    @Transactional
    public void softDelete(Long meId) {
        feedLikeRepository.deleteByUserId(meId);
        feedBookmarkRepository.deleteByUserId(meId);
        feedService.deleteAllByAuthorId(meId);
        User me = loadActiveForWrite(meId);
        me.softDelete(Instant.now(clock));
        me.bumpTokenVersion();
        refreshTokenService.deleteAll(me.getId());
    }

    @Transactional
    public FollowResponseDto follow(Long meId, Long targetId) {
        if (meId.equals(targetId)) throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
        User target = userRepository.findById(targetId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (target.getStatus() == UserStatus.SUSPENDED) throw new BusinessException(ErrorCode.USER_INACTIVE);
        if (target.getStatus() == UserStatus.DELETED) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (userBlockRepo.existsMeBlockedTarget(meId, targetId))
            throw new BusinessException(ErrorCode.FOLLOW_NOT_ALLOWED_YOU_BLOCKED_TARGET);
        if (userFollowRepo.existsByFollowerIdAndFollowingId(meId, targetId))
            throw new BusinessException(ErrorCode.FOLLOW_ALREADY_EXISTS);

        UserFollow f = UserFollow.builder()
                .follower(em.getReference(User.class, meId))
                .following(em.getReference(User.class, targetId))
                .build();
        userFollowRepo.save(f);
        return new FollowResponseDto(meId, targetId, true);
    }

    @Transactional
    public void unfollow(Long meId, Long targetId) {
        userFollowRepo.deleteLink(meId, targetId);
    }

    @Transactional
    public BlockResponseDto block(Long meId, Long targetId) {
        if (meId.equals(targetId)) throw new BusinessException(ErrorCode.CANNOT_BLOCK_SELF);
        User target = userRepository.findById(targetId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (target.getStatus() == UserStatus.SUSPENDED) throw new BusinessException(ErrorCode.USER_INACTIVE);
        if (target.getStatus() == UserStatus.DELETED) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (userBlockRepo.existsMeBlockedTarget(meId, targetId)) throw new BusinessException(ErrorCode.BLOCK_ALREADY_EXISTS);

        userFollowRepo.deleteLink(meId, targetId);
        userFollowRepo.deleteLink(targetId, meId);

        UserBlock b = UserBlock.builder()
                .blocker(em.getReference(User.class, meId))
                .blocked(em.getReference(User.class, targetId))
                .build();
        userBlockRepo.save(b);
        return new BlockResponseDto(meId, targetId, true);
    }

    @Transactional
    public void unblock(Long meId, Long targetId) {
        userBlockRepo.deleteLink(meId, targetId);
    }

    @Transactional(readOnly = true)
    public PageCursorResponse<UserSummaryDto> listFollowers(Long userId, Long cursor, int size) {
        int pageSize = Math.max(1, Math.min(size, 50));

        var edges = (cursor == null)
                ? userFollowRepo.findByFollowingIdOrderByIdDesc(userId, PageRequest.of(0, pageSize))
                : userFollowRepo.findByFollowingIdAndIdLessThanOrderByIdDesc(userId, cursor, PageRequest.of(0, pageSize));

        var items = edges.stream()
                .map(e -> userMapper.toSummary(e.getFollower()))
                .toList();

        String next = edges.size() == pageSize
                ? String.valueOf(edges.get(edges.size() - 1).getId())
                : null;
        return new PageCursorResponse<>(items, next, next != null);
    }

    @Transactional(readOnly = true)
    public PageCursorResponse<UserSummaryDto> listFollowings(Long userId, Long cursor, int size) {
        int pageSize = Math.max(1, Math.min(size, 50));
        var edges = (cursor == null)
                ? userFollowRepo.findByFollowerIdOrderByIdDesc(userId, PageRequest.of(0, pageSize))
                : userFollowRepo.findByFollowerIdAndIdLessThanOrderByIdDesc(userId, cursor, PageRequest.of(0, pageSize));

        var items = edges.stream()
                .map(e -> userMapper.toSummary(e.getFollowing()))
                .toList();

        String next = edges.size() == pageSize
                ? String.valueOf(edges.get(edges.size() - 1).getId())
                : null;
        return new PageCursorResponse<>(items, next, next != null);
    }

    @Transactional(readOnly = true)
    public PageCursorResponse<UserSummaryDto> listMyBlocks(Long meId, Long cursor, int size) {
        int pageSize = Math.max(1, Math.min(size, 50));
        var edges = userBlockRepo.findBlocks(meId, cursor, PageRequest.of(0, pageSize));
        var items = edges.stream().map(e -> userMapper.toSummary(e.getBlocked())).toList();
        String next = edges.size() == pageSize ? String.valueOf(edges.get(edges.size() - 1).getId()) : null;
        return new PageCursorResponse<>(items, next, next != null);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(String username, String email) {
        Boolean u = username == null ? null : !userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(username);
        Boolean e = email == null ? null : !userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email);
        return new AvailabilityResponse(u, e);
    }

    private User loadActiveForWrite(Long userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (u.getStatus() == UserStatus.SUSPENDED) throw new BusinessException(ErrorCode.USER_INACTIVE);
        if (u.getStatus() == UserStatus.DELETED) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        return u;
    }

    public record AvailabilityResponse(Boolean usernameAvailable, Boolean emailAvailable) {
    }

    private String mapImageExtFromMime(String mime) {
        if (mime == null) return null;
        return switch (mime.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/avif" -> "avif";
            default -> null;
        };
    }

    private String buildProfileImageKey(Long userId, String ext) {
        long now = Instant.now(clock).toEpochMilli();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "users/%d/profile/%d_%s.%s".formatted(userId, now, uuid, ext);
    }

    private String createPresignedPutUrl(String objectKey, String mimeType, Long sizeBytes) {
        PutObjectRequest.Builder putReq = PutObjectRequest.builder()
                .bucket(mediaBucket)
                .key(objectKey)
                .contentType(mimeType);

        PutObjectRequest por = putReq.build();

        PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                .putObjectRequest(por)
                .signatureDuration(Duration.ofMinutes(10))
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presign);
        return presigned.url().toString();
    }
}
