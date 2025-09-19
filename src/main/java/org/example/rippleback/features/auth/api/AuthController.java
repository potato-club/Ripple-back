package org.example.rippleback.features.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.security.jwt.JwtPrincipal;
import org.example.rippleback.features.auth.api.dto.AccessTokenResponseDto;
import org.example.rippleback.features.auth.api.dto.LoginRequestDto;
import org.example.rippleback.features.auth.api.dto.TokenRequestDto;
import org.example.rippleback.features.auth.api.support.RefreshCookieSupport;
import org.example.rippleback.features.auth.app.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieSupport refreshCookieSupport;

    @Operation(summary = "로그인", description = "username/email + password로 로그인, 토큰 발급")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "자격 증명 오류 (1100)"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 (9000)")
    })
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response) {
        var result = authService.login(request);
        refreshCookieSupport.writeFromToken(response, result.refreshToken()); // ← 여기만!
        return ResponseEntity.ok(new AccessTokenResponseDto(result.accessToken()));
    }

    @Operation(
            summary = "토큰 재발급",
            description = "HttpOnly 쿠키의 RefreshToken으로 새로운 AccessToken을 재발급합니다. 요청 바디는 없이, 헤더에 X-Device-Id만 포함하세요."
    )

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰 유효하지 않음 (1105/1109)")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponseDto> refresh(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader("X-Device-Id") String deviceId) {
        String rt = refreshCookieSupport.read(request);

        var result = authService.refresh(new TokenRequestDto(rt, deviceId));
        refreshCookieSupport.writeFromToken(response, result.refreshToken());
        return ResponseEntity.ok(new AccessTokenResponseDto(result.accessToken()));
    }

    @Operation(summary = "로그아웃", description = "특정 디바이스에서 로그아웃 (RefreshToken 무효화)")
    @ApiResponse(responseCode = "204", description = "로그아웃 성공")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId,
            HttpServletResponse response) {
        authService.logout(principal.userId(), deviceId);
        refreshCookieSupport.clear(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout/all")
    public ResponseEntity<Void> logoutAll(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletResponse response) {
        authService.logoutAll(principal.userId());
        refreshCookieSupport.clear(response);
        return ResponseEntity.noContent().build();
    }
}
