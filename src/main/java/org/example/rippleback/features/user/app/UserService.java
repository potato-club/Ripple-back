package org.example.rippleback.features.user.app;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.exceptions.auth.InvalidCredentialsException;
import org.example.rippleback.core.error.exceptions.user.*;
import org.example.rippleback.features.media.domain.Media;
import org.example.rippleback.features.media.domain.MediaStatus;
import org.example.rippleback.features.media.domain.MediaType;
import org.example.rippleback.features.media.infra.MediaRepository;
import org.example.rippleback.features.user.api.dto.*;
import org.example.rippleback.features.user.domain.Block;
import org.example.rippleback.features.user.domain.Follow;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.features.user.domain.UserStatus;
import org.example.rippleback.features.user.infra.BlockRepository;
import org.example.rippleback.features.user.infra.FollowRepository;
import org.example.rippleback.features.user.infra.UserRepository;
import org.example.rippleback.infra.redis.RefreshTokenService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final MediaRepository mediaRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final Clock clock;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public SignupResponseDto signup(SignupRequestDto req) {
        String normalizedEmail = req.email().trim().toLowerCase(Locale.ROOT);

        if (!emailVerificationService.isVerified(normalizedEmail)) {
            throw new EmailNotVerifiedException();
        }

        if (userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(req.username())) {
            throw new DuplicateUsernameException();
        }

        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new DuplicateEmailException();
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

    public MeResponseDto getMe(Long meId) {
        User u = userRepository.findById(meId).orElseThrow(UserNotFoundException::new);
        return userMapper.toMe(u);
    }

    public UserResponseDto getProfileById(Long id) {
        User u = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        if (u.getStatus() != UserStatus.ACTIVE) throw new UserNotFoundException();
        return userMapper.toProfile(u);
    }

    public UserResponseDto getProfileByUsername(String username) {
        User u = userRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(username).orElseThrow(UserNotFoundException::new);
        if (u.getStatus() != UserStatus.ACTIVE) throw new UserNotFoundException();
        return userMapper.toProfile(u);
    }

    public PageCursorResponse<UserSummaryDto> search(String q, Long cursor, int size) {
        int pageSize = Math.max(1, Math.min(size, 50));
        List<User> users = userRepository.search(q == null ? "" : q, cursor, PageRequest.of(0, pageSize));
        String next = users.size() == pageSize ? String.valueOf(users.get(users.size() - 1).getId()) : null;
        boolean hasNext = next != null;
        List<UserSummaryDto> items = users.stream().map(userMapper::toSummary).toList();
        return new PageCursorResponse<>(items, next, hasNext);
    }

    @Transactional
    public UserResponseDto updateProfile(Long meId, UpdateProfileRequestDto req) {
        User me = loadActiveForWrite(meId);

        if (req.username() != null) {
            String nu = req.username().strip();
            if (!nu.equalsIgnoreCase(me.getUsername())) {
                if (userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(nu)) {
                    throw new IllegalArgumentException("duplicate username"); // TODO 교체
                }
                me.changeUsername(nu);
            }
        }
        if (req.profileMessage() != null) {
            me.changeProfileMessage(req.profileMessage());
        }

        var img = req.profileImage();
        var action = (img == null || img.action() == null)
                ? UpdateProfileRequestDto.ProfileImagePatch.Action.KEEP
                : img.action();

        switch (action) {
            case SET -> {
                Long mediaId = img.mediaId();
                if (mediaId == null) throw new IllegalArgumentException("mediaId required"); // TODO
                var m = mediaRepository.findById(mediaId)
                        .orElseThrow(() -> new IllegalArgumentException("media not found")); // TODO
                // TODO: owner == meId, type == IMAGE, status == READY 검증
                me.updateProfileImage(m.getId());
            }
            case CLEAR -> me.clearProfileImage();
            case KEEP -> { /* no-op */ }
        }

        return userMapper.toProfile(me);
    }



    @Transactional
    public void changePassword(Long meId, String current, String newPw) {
        User me = loadActiveForWrite(meId);
        if (!passwordEncoder.matches(current, me.getPassword())) throw new InvalidCredentialsException();
        me.changePassword(passwordEncoder.encode(newPw));
    }

    @Transactional
    public void softDelete(Long meId) {
        User me = loadActiveForWrite(meId);
        me.softDelete(Instant.now(clock));
        me.bumpTokenVersion();
        refreshTokenService.deleteAll(me.getId());
    }

    @Transactional
    public FollowResponseDto follow(Long meId, Long targetId) {
        if (meId.equals(targetId)) throw new CannotFollowSelfException();
        User target = userRepository.findById(targetId).orElseThrow(UserNotFoundException::new);
        if (target.getStatus() == UserStatus.SUSPENDED) throw new UserInactiveException();
        if (target.getStatus() == UserStatus.DELETED) throw new UserNotFoundException();
        if (blockRepository.existsMeBlockedTarget(meId, targetId))
            throw new FollowNotAllowedYouBlockedTargetException();
        if (followRepository.existsLink(meId, targetId)) throw new FollowAlreadyExistsException();
        Follow f = Follow.builder()
                .follower(em.getReference(User.class, meId))
                .following(em.getReference(User.class, targetId))
                .build();
        followRepository.save(f);
        return new FollowResponseDto(meId, targetId, true);
    }

    @Transactional
    public void unfollow(Long meId, Long targetId) {
        followRepository.deleteLink(meId, targetId);
    }

    @Transactional
    public BlockResponseDto block(Long meId, Long targetId) {
        if (meId.equals(targetId)) throw new CannotBlockSelfException();
        User target = userRepository.findById(targetId).orElseThrow(UserNotFoundException::new);
        if (target.getStatus() == UserStatus.SUSPENDED) throw new UserInactiveException();
        if (target.getStatus() == UserStatus.DELETED) throw new UserNotFoundException();
        if (blockRepository.existsMeBlockedTarget(meId, targetId)) throw new BlockAlreadyExistsException();
        followRepository.deleteLink(meId, targetId);
        followRepository.deleteLink(targetId, meId);
        Block b = Block.builder()
                .blocker(em.getReference(User.class, meId))
                .blocked(em.getReference(User.class, targetId))
                .build();
        blockRepository.save(b);
        return new BlockResponseDto(meId, targetId, true);
    }

    @Transactional
    public void unblock(Long meId, Long targetId) {
        blockRepository.deleteLink(meId, targetId);
    }

    public PageCursorResponse<UserSummaryDto> listFollowers(Long userId, Long cursor, int size) {
        int pageSize = Math.max(1, Math.min(size, 50));
        var edges = followRepository.findFollowers(userId, cursor, PageRequest.of(0, pageSize));
        var items = edges.stream().map(e -> userMapper.toSummary(e.getFollower())).toList();
        String next = edges.size() == pageSize ? String.valueOf(edges.get(edges.size() - 1).getId()) : null;
        return new PageCursorResponse<>(items, next, next != null);
    }

    public PageCursorResponse<UserSummaryDto> listFollowings(Long userId, Long cursor, int size) {
        int pageSize = Math.max(1, Math.min(size, 50));
        var edges = followRepository.findFollowings(userId, cursor, PageRequest.of(0, pageSize));
        var items = edges.stream().map(e -> userMapper.toSummary(e.getFollowing())).toList();
        String next = edges.size() == pageSize ? String.valueOf(edges.get(edges.size() - 1).getId()) : null;
        return new PageCursorResponse<>(items, next, next != null);
    }

    public PageCursorResponse<UserSummaryDto> listMyBlocks(Long meId, Long cursor, int size) {
        int pageSize = Math.max(1, Math.min(size, 50));
        var edges = blockRepository.findBlocks(meId, cursor, PageRequest.of(0, pageSize));
        var items = edges.stream().map(e -> userMapper.toSummary(e.getBlocked())).toList();
        String next = edges.size() == pageSize ? String.valueOf(edges.get(edges.size() - 1).getId()) : null;
        return new PageCursorResponse<>(items, next, next != null);
    }

    public AvailabilityResponse availability(String username, String email) {
        Boolean u = username == null ? null : !userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(username);
        Boolean e = email == null ? null : !userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email);
        return new AvailabilityResponse(u, e);
    }

    private User loadActiveForWrite(Long userId) {
        User u = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (u.getStatus() == UserStatus.SUSPENDED) throw new UserInactiveException();
        if (u.getStatus() == UserStatus.DELETED) throw new UserNotFoundException();
        return u;
    }

    public record AvailabilityResponse(Boolean usernameAvailable, Boolean emailAvailable) {
    }
}
