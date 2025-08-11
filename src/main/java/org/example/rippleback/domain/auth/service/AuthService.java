package org.example.rippleback.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.domain.auth.dto.LoginRequestDto;
import org.example.rippleback.domain.auth.dto.LoginResponseDto;
import org.example.rippleback.domain.auth.dto.TokenRequestDto;
import org.example.rippleback.domain.auth.dto.TokenResponseDto;
import org.example.rippleback.domain.user.entity.User;
import org.example.rippleback.domain.user.repository.UserRepository;
import org.example.rippleback.global.security.JwtTokenProvider;
import org.example.rippleback.global.security.RefreshTokenService;
import org.example.rippleback.global.security.TokenHash;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        Long userId = user.getId();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User not active");
        }
        long ver = user.getTokenVersion();

        String access = jwtTokenProvider.createAccessToken(userId, ver);
        String refresh = jwtTokenProvider.createRefreshToken(userId, ver);

        String jti = jwtTokenProvider.getJti(refresh);
        Instant exp = jwtTokenProvider.getExpiration(refresh);
        long remainMs = Duration.between(Instant.now(), exp).toMillis();
        String hash = TokenHash.sha256(refresh);
        refreshTokenService.store(userId, request.deviceId(), jti, hash, remainMs);

        user.setLastLoginAt(Instant.now());

        return new LoginResponseDto(access, refresh);
    }

    @Transactional
    public TokenResponseDto refresh(TokenRequestDto request) {
        String provided = request.refreshToken();

        if (!jwtTokenProvider.isValid(provided) || !"refresh".equals(jwtTokenProvider.getTyp(provided))) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        Long userId = jwtTokenProvider.getUserId(provided);
        long ver = jwtTokenProvider.getVersion(provided);
        String jti = jwtTokenProvider.getJti(provided);
        Instant exp = jwtTokenProvider.getExpiration(provided);
        long remainMs = Duration.between(Instant.now(), exp).toMillis();
        if (remainMs <= 0) throw new BadCredentialsException("Refresh token expired");

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
        String newRefresh = jwtTokenProvider.createRefreshToken(userId, ver);

        refreshTokenService.markUsed(jti, remainMs);
        String newJti = jwtTokenProvider.getJti(newRefresh);
        Instant newExp = jwtTokenProvider.getExpiration(newRefresh);
        long newRemainMs = Duration.between(Instant.now(), newExp).toMillis();
        String newHash = TokenHash.sha256(newRefresh);
        refreshTokenService.store(userId, request.deviceId(), newJti, newHash, newRemainMs);

        return new TokenResponseDto(newAccess, newRefresh);
    }

    @Transactional
    public void logout(String accessToken, String deviceId) {
        if (!jwtTokenProvider.isValid(accessToken) || !"access".equals(jwtTokenProvider.getTyp(accessToken))) {
            return;
        }
        Long userId = jwtTokenProvider.getUserId(accessToken);
        refreshTokenService.delete(userId, deviceId);
    }

    @Transactional
    public void logoutAll(String accessToken) {
        if (!jwtTokenProvider.isValid(accessToken) || !"access".equals(jwtTokenProvider.getTyp(accessToken))) {
            return;
        }
        Long userId = jwtTokenProvider.getUserId(accessToken);
        refreshTokenService.deleteAll(userId);
    }
}