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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    /**
     * 로그인: 이메일/비밀번호 검증 → 액세스/리프레시 발급
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                request.email(), request.password()
        );
        authenticationManager.authenticate(authToken); // DaoAuthenticationProvider가 검증

        // 사용자 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        Long userId = user.getId();
        long ver = 0L; // TODO: user.getTokenVersion() 필드 추가 후 적용
        // TODO: 상태 체크 - user.getStatus() == ACTIVE

        // 새 AT/RT 발급(Provider 신규 메서드 사용)
        String access = jwtTokenProvider.createAccessToken(userId, ver);
        String refresh = jwtTokenProvider.createRefreshToken(userId, ver);

        // RT 파싱 → 남은 만료 → 해시 저장(화이트리스트)
        String jti = jwtTokenProvider.getJti(refresh);
        Instant exp = jwtTokenProvider.getExpiration(refresh);
        long remainMs = Duration.between(Instant.now(), exp).toMillis();
        String hash = TokenHash.sha256(refresh);
        refreshTokenService.store(userId, jti, hash, remainMs);

        // 마지막 로그인 시간 갱신은 User 엔티티 확정 후 적용
        return new LoginResponseDto(access, refresh);
    }

    /**
     * 회전 토큰: RT 검증/재사용 감지/화이트리스트 일치 → Access/Refresh 재발급 + 회전 처리
     */
    @Transactional
    public TokenResponseDto refresh(TokenRequestDto request) {
        String provided = request.refreshToken();

        // 1) 형식/서명/만료 검증 + typ=refresh
        if (!jwtTokenProvider.isValid(provided) || !"refresh".equals(jwtTokenProvider.getTyp(provided))) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        Long userId = jwtTokenProvider.getUserId(provided);
        long ver = jwtTokenProvider.getVersion(provided);
        String jti = jwtTokenProvider.getJti(provided);
        Instant exp = jwtTokenProvider.getExpiration(provided);
        long remainMs = Duration.between(Instant.now(), exp).toMillis();
        if (remainMs <= 0) throw new BadCredentialsException("Refresh token expired");

        // 2) 재사용 탐지
        if (refreshTokenService.isUsed(jti)) {
            // TODO: user.tokenVersion++ (세션 전체 무효화) - 필드 추가 후 적용
            throw new BadCredentialsException("Detected refresh token reuse");
        }

        // 3) 화이트리스트 일치 확인(저장된 {jti, hash}와 모두 일치해야 함)
        String hash = TokenHash.sha256(provided);
        var entryOpt = refreshTokenService.get(userId);
        if (entryOpt.isEmpty()) throw new BadCredentialsException("Refresh token not found");
        var entry = entryOpt.get();
        if (!entry.jti().equals(jti) || !entry.hash().equals(hash)) {
            refreshTokenService.delete(userId); // 의심 케이스: 화이트리스트 제거
            throw new BadCredentialsException("Refresh token mismatch");
        }

        // 4) 사용자 상태/버전 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        // TODO: if (user.getStatus() != UserStatus.ACTIVE) -> 403
        // TODO: if (user.getTokenVersion() != ver) -> 401 (세션 무효)

        // 5) 회전: 새 AT/RT 발급
        String newAccess = jwtTokenProvider.createAccessToken(userId, ver);
        String newRefresh = jwtTokenProvider.createRefreshToken(userId, ver);

        // 6) 이전 jti 마킹 + 새 RT 저장
        refreshTokenService.markUsed(jti, remainMs);
        String newJti = jwtTokenProvider.getJti(newRefresh);
        Instant newExp = jwtTokenProvider.getExpiration(newRefresh);
        long newRemainMs = Duration.between(Instant.now(), newExp).toMillis();
        String newHash = TokenHash.sha256(newRefresh);
        refreshTokenService.store(userId, newJti, newHash, newRemainMs);

        return new TokenResponseDto(newAccess, newRefresh);
    }

    /**
     * 로그아웃: RefreshToken 삭제 (+ 선택: AccessToken 블랙리스트)
     * accessToken은 "Bearer ..." 형태로 들어온 원문 토큰 문자열
     */
    @Transactional
    public void logout(String accessToken) {
        if (!jwtTokenProvider.isValid(accessToken) || !"access".equals(jwtTokenProvider.getTyp(accessToken))) {
            return;
        }
        Long userId = jwtTokenProvider.getUserId(accessToken);
        refreshTokenService.delete(userId);
        // TODO: 필요 시 AccessToken 블랙리스트 등록 (만료까지 TTL 저장)
    }
}