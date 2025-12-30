package org.example.rippleback.features.user.app;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import org.example.rippleback.features.feed.app.FeedService;
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
                Media saved = mediaRepo.saveAndFlush(media);

                me.updateProfileImage(saved.getId());
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

    // 팔로우 하기 전 제약조건 확인하고 팔로우 생성
    @Transactional
    public FollowResponseDto follow(Long meId, Long targetId) {
        if (meId.equals(targetId)) throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);

        // 대상이 되는 유저 상태 체크
        loadActiveForWrite(targetId);

        ensureNoBlockBetween(meId, targetId);

        if (userFollowRepo.existsByFollowerIdAndFollowingId(meId, targetId))
            throw new BusinessException(ErrorCode.FOLLOW_ALREADY_EXISTS);

        UserFollow f = UserFollow.builder()
                .follower(em.getReference(User.class, meId))
                .following(em.getReference(User.class, targetId))
                .build();
        userFollowRepo.save(f);
        return new FollowResponseDto(meId, targetId, true);
    }

    // 팔로우 해제는 데이터 삭제 (멱등조치 Repo에서 해둠)
    @Transactional
    public void unfollow(Long meId, Long targetId) {
        userFollowRepo.deleteLink(meId, targetId);
    }

    // 차단하기 전에 제약조건 확인 후 차단 생성
    @Transactional
    public BlockResponseDto block(Long meId, Long targetId) {
        // 자체 차단 금지
        if (meId.equals(targetId)) throw new BusinessException(ErrorCode.CANNOT_BLOCK_SELF);

        // 대상이 되는 유저 상태 체크
        loadActiveForWrite(targetId);

        // 이미 차단 관계 존재하면 차단 불가능
        ensureNotAlreadyBlockedByMe(meId, targetId);

        // 차단 만들어지면 서로 팔로우 관계 해제
        userFollowRepo.deleteLink(meId, targetId);
        userFollowRepo.deleteLink(targetId, meId);

        // 나와 상대방 users 테이블 업데이트
        UserBlock b = UserBlock.builder()
                .blocker(em.getReference(User.class, meId))
                .blocked(em.getReference(User.class, targetId))
                .build();
        userBlockRepo.save(b);
        return new BlockResponseDto(meId, targetId, true);
    }

    // 차단 해제는 데이터 삭제 (Repo에서 멱등조건 해둠)
    @Transactional
    public void unblock(Long meId, Long targetId) {
        userBlockRepo.deleteBlockLink(meId, targetId);
    }

    // 유저의 팔로우 한 목록 조회 (보는 유저가 viewer, 목록 보여주는 유저가 ownerId)
    @Transactional(readOnly = true)
    public PageCursorResponse<UserSummaryDto> listFollowers(Long viewerId, Long userId, Long cursor, int size) {
        // 페이지를 size 받은거 혹은 20 중에 최소값으로 사용
        int pageSize = Math.max(1, Math.min(size, 20));

        // 목록 보려는 유저가 존재하는 유저인지 확인
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 목록 보려는 유저 계정이 활성화 상태인지 확인
        if (owner.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }

        // 목록을 보려는 유저와 목록 보이는 유저가 차단 관계 있으면 예외 반환 (프로필에서도 설정하지만 여기서도 설정)
        if (userBlockRepo.existsAnyBlock(viewerId, userId)) {
            throw new BusinessException(ErrorCode.BLOCK_ALREADY_EXISTS);
        }

        // 3) 목록 아이템은 "viewer 기준으로 차단 관계가 있는 팔로워는 제외"한 DB 결과 사용
        var edges = userFollowRepo.findFollowersVisibleTo(
                userId,
                viewerId,
                cursor,
                PageRequest.of(0, pageSize)
        );

        var items = edges.stream()
                .map(e -> userMapper.toSummary(e.getFollower()))
                .toList();

        String next = edges.size() == pageSize
                ? String.valueOf(edges.get(edges.size() - 1).getId())
                : null;

        return new PageCursorResponse<>(items, next, next != null);
    }


    @Transactional(readOnly = true)
    public PageCursorResponse<UserSummaryDto> listMyBlocks(Long meId, Long cursor, int size) {
        // 내 상태 체크
        loadActiveForWrite(meId);

        int pageSize = Math.max(1, Math.min(size, 50));
        var edges = userBlockRepo.findBlocks(meId, cursor, PageRequest.of(0, pageSize));

        var items = edges.stream()
                .map(UserBlock::getBlocked)
                .map(userMapper::toSummary)
                .toList();

        String next = edges.size() == pageSize
                ? String.valueOf(edges.get(edges.size() - 1).getId())
                : null;

        return new PageCursorResponse<>(items, next, next != null);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(String username, String email) {
        Boolean u = username == null
                ? null
                : !userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(username);
        Boolean e = email == null
                ? null
                : !userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email);
        return new AvailabilityResponse(u, e);
    }

    private User loadActiveForWrite(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (u.getStatus() != UserStatus.ACTIVE) throw new BusinessException(ErrorCode.USER_INACTIVE);
        return u;
    }

    // 프로필, 피드, 댓글, DM 의 경우에 서로간에 Block 존재하면 상대방 데이터 안보임
    // 그걸 위해서 이걸로 체크하고 넘어감
    private void ensureNoBlockBetween(Long meId, Long targetId) {
        if (userBlockRepo.existsAnyBlock(meId, targetId)) {
            throw new BusinessException(ErrorCode.BLOCK_ALREADY_EXISTS);
        }
    }

    // 내가 상대방 차단 했는지 확인하기 위한 메서드
    private void ensureNotAlreadyBlockedByMe(Long meId, Long targetId) {
        if (userBlockRepo.existsMeBlockedTarget(meId, targetId)) {
            throw new BusinessException(ErrorCode.BLOCK_ALREADY_EXISTS);
        }
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
