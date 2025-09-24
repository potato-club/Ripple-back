package org.example.rippleback.features.auth.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.exceptions.auth.*;
import org.example.rippleback.core.error.exceptions.user.UserInactiveException;
import org.example.rippleback.core.error.exceptions.user.UserNotFoundException;
import org.example.rippleback.features.auth.api.dto.LoginRequestDto;
import org.example.rippleback.features.auth.api.dto.LoginResponseDto;
import org.example.rippleback.features.auth.api.dto.TokenRequestDto;
import org.example.rippleback.features.auth.api.dto.TokenResponseDto;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.features.user.domain.UserStatus;
import org.example.rippleback.features.user.infra.UserRepository;
import org.example.rippleback.core.security.jwt.JwtTokenProvider;
import org.example.rippleback.infra.redis.RefreshTokenService;
import org.example.rippleback.core.security.jwt.TokenHash;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

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
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != UserStatus.ACTIVE) throw new UserInactiveException();

        Long userId = user.getId();
        long ver = user.getTokenVersion();

        String access = jwtTokenProvider.createAccessToken(userId, ver);
        String refresh = jwtTokenProvider.createRefreshToken(userId, ver, request.deviceId());

        var c = jwtTokenProvider.decode(refresh);
        long remainMs = Duration.between(Instant.now(clock), c.exp()).toMillis();
        String hash = TokenHash.sha256(refresh);
        refreshTokenService.store(userId, request.deviceId(), c.jti(), hash, remainMs);

        user.touchLastLogin(Instant.now(clock)); // ← 세터 대신 도메인 메서드

        return new LoginResponseDto(access, refresh);
    }

    @Transactional
    public TokenResponseDto refresh(TokenRequestDto request) {
        String provided = request.refreshToken();

        var c = decodeRefreshOrThrow(provided);

        Long userId = c.userId();
        long ver = c.version();
        String jti = c.jti();
        long remainMs = Duration.between(Instant.now(clock), c.exp()).toMillis();
        if (remainMs <= 0) throw new TokenExpiredException();

        String tokenDevice = c.deviceId();
        if (tokenDevice == null || !tokenDevice.equals(request.deviceId())) {
            throw new DeviceMismatchException();
        }

        if (refreshTokenService.isUsed(jti)) {
            userRepository.incrementTokenVersion(userId);
            throw new RefreshReusedException();
        }

        String hash = TokenHash.sha256(provided);
        var entryOpt = refreshTokenService.get(userId, request.deviceId());
        if (entryOpt.isEmpty()) throw new RefreshNotFoundException();
        var entry = entryOpt.get();
        if (!entry.jti().equals(jti) || !entry.hash().equals(hash)) {
            refreshTokenService.delete(userId, request.deviceId());
            throw new RefreshMismatchException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserInactiveException();
        }
        if (user.getTokenVersion() != ver) {
            throw new SessionInvalidatedException();
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
    public void logout(Long userId, String deviceId) {
        refreshTokenService.delete(userId, deviceId);
    }

    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenService.deleteAll(userId);
    }

    private JwtTokenProvider.TokenClaims decodeRefreshOrThrow(String token) {
        if (token == null || token.isBlank()) {     // ★ 추가
            throw new TokenMissingException();
        }
        try {
            var c = jwtTokenProvider.decode(token);
            if (!"refresh".equals(c.tokenType())) {
                throw new TokenTypeInvalidException();
            }
            return c;
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidException();
        }
    }

}
