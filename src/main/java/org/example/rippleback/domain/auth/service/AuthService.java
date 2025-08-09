package org.example.rippleback.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.domain.auth.dto.LoginRequestDto;
import org.example.rippleback.domain.auth.dto.LoginResponseDto;
import org.example.rippleback.domain.auth.dto.TokenRequestDto;
import org.example.rippleback.domain.auth.dto.TokenResponseDto;
import org.example.rippleback.global.security.JwtTokenProvider;
import org.example.rippleback.global.security.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * 로그인: 이메일/비밀번호 검증 → 액세스/리프레시 발급
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                request.email(), request.password()
        );
        Authentication authentication = authenticationManager.authenticate(authToken); // DaoAuthenticationProvider가 검증

        // 검증 성공 시 토큰 발급
        String access = jwtTokenProvider.createAccessToken(request.email());
        String refresh = jwtTokenProvider.createRefreshToken(request.email()); // 내부에서 Redis 저장(TTL)

        return new LoginResponseDto(access, refresh);
    }

    /**
     * 회전 토큰: 유효 Refresh 일치 → Access 재발급 + 새 Refresh 발급 & 저장(덮어쓰기)
     */
    @Transactional
    public TokenResponseDto refresh(TokenRequestDto request) {
        String email = request.email();
        String provided = request.refreshToken();

        String saved = refreshTokenService.get(email);
        if (saved == null) {
            throw new BadCredentialsException("Refresh token expired or not found");
        }
        if (!saved.equals(provided)) {
            // 탈취/재사용 시도 가능성 → 바로 차단
            refreshTokenService.delete(email);
            throw new BadCredentialsException("Invalid refresh token");
        }

        String newAccess = jwtTokenProvider.createAccessToken(email);
        String newRefresh = jwtTokenProvider.createRefreshToken(email); // 내부에서 Redis 저장(TTL 포함)

        return new TokenResponseDto(newAccess, newRefresh);
    }

    /**
     * 로그아웃: RefreshToken 삭제 (+ 선택: AccessToken 블랙리스트)
     * accessToken은 "Bearer ..." 형태로 들어온 원문 토큰 문자열
     */
    @Transactional
    public void logout(String accessToken) {
        // TODO: 필요 시 AccessToken 블랙리스트 등록 (만료까지 TTL 저장)
        // 현재는 간단히 해당 사용자의 RefreshToken만 삭제
        String email = jwtTokenProvider.getEmailFromToken(accessToken);
        refreshTokenService.delete(email);
    }
}