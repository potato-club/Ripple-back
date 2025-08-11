package org.example.rippleback.features.auth.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.auth.api.dto.LoginRequestDto;
import org.example.rippleback.features.auth.api.dto.LoginResponseDto;
import org.example.rippleback.features.auth.api.dto.TokenRequestDto;
import org.example.rippleback.features.auth.api.dto.TokenResponseDto;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.domain.user.entity.UserStatus;
import org.example.rippleback.features.user.infra.UserRepository;
import org.example.rippleback.core.security.jwt.JwtTokenProvider;
import org.example.rippleback.infra.redis.RefreshTokenService;
import org.example.rippleback.core.security.jwt.TokenHash;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User not active");
        }

        Long userId = user.getId();
        long ver = user.getTokenVersion();

        String access = jwtTokenProvider.createAccessToken(userId, ver);
        String refresh = jwtTokenProvider.createRefreshToken(userId, ver, request.deviceId());

        var c = jwtTokenProvider.decode(refresh);
        long remainMs = Duration.between(Instant.now(clock), c.exp()).toMillis();
        String hash = TokenHash.sha256(refresh);
        refreshTokenService.store(userId, request.deviceId(), c.jti(), hash, remainMs);

        user.setLastLoginAt(Instant.now(clock));

        return new LoginResponseDto(access, refresh);
    }

    @Transactional
    public TokenResponseDto refresh(TokenRequestDto request) {
        String provided = request.refreshToken();

        var c = jwtTokenProvider.decode(provided);
        if (!"refresh".equals(c.tokenType())) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        Long userId = c.userId();
        long ver = c.version();
        String jti = c.jti();
        long remainMs = Duration.between(Instant.now(clock), c.exp()).toMillis();
        if (remainMs <= 0) throw new BadCredentialsException("Refresh token expired");

        String tokenDevice = c.deviceId();
        if (tokenDevice == null || !tokenDevice.equals(request.deviceId())) {
            throw new BadCredentialsException("Device mismatch");
        }

        if (refreshTokenService.isUsed(jti)) {
            userRepository.incrementTokenVersion(userId);
            throw new BadCredentialsException("Detected refresh token reuse");
        }

        String hash = TokenHash.sha256(provided);
        var entryOpt = refreshTokenService.get(userId, request.deviceId());
        if (entryOpt.isEmpty()) throw new BadCredentialsException("Refresh token not found");
        var entry = entryOpt.get();
        if (!entry.jti().equals(jti) || !entry.hash().equals(hash)) {
            refreshTokenService.delete(userId, request.deviceId());
            throw new BadCredentialsException("Refresh token mismatch");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User not active");
        }
        if (user.getTokenVersion() != ver) {
            throw new BadCredentialsException("Session invalidated");
        }

        String newAccess = jwtTokenProvider.createAccessToken(userId, ver);
        String newRefresh = jwtTokenProvider.createRefreshToken(userId, ver, request.deviceId());

        refreshTokenService.markUsed(jti, remainMs);

        var nc = jwtTokenProvider.decode(newRefresh);
        long newRemainMs = Duration.between(Instant.now(clock), nc.exp()).toMillis();
        String newHash = TokenHash.sha256(newRefresh);
        refreshTokenService.store(userId, request.deviceId(), nc.jti(), newHash, newRemainMs);

        return new TokenResponseDto(newAccess, newRefresh);
    }

    @Transactional
    public void logout(String accessToken, String deviceId) {
        try {
            var c = jwtTokenProvider.decode(accessToken);
            if (!"access".equals(c.tokenType())) return;
            refreshTokenService.delete(c.userId(), deviceId);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void logoutAll(String accessToken) {
        try {
            var c = jwtTokenProvider.decode(accessToken);
            if (!"access".equals(c.tokenType())) return;
            refreshTokenService.deleteAll(c.userId());
        } catch (Exception ignored) {
        }
    }
}
