package org.example.rippleback.features.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.security.jwt.JwtPrincipal;
import org.example.rippleback.features.auth.api.dto.LoginRequestDto;
import org.example.rippleback.features.auth.api.dto.LoginResponseDto;
import org.example.rippleback.features.auth.api.dto.TokenRequestDto;
import org.example.rippleback.features.auth.api.dto.TokenResponseDto;
import org.example.rippleback.features.auth.app.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "username/email + password로 로그인, 토큰 발급")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "자격 증명 오류 (1100)"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 (9000)")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "토큰 재발급", description = "RefreshToken으로 새로운 AccessToken/RefreshToken 재발급")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰 유효하지 않음 (1105/1109)")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@Valid @RequestBody TokenRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "로그아웃", description = "특정 디바이스에서 로그아웃 (RefreshToken 무효화)")
    @ApiResponse(responseCode = "204", description = "로그아웃 성공")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId) {
        authService.logout(principal.userId(), deviceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "전체 로그아웃", description = "해당 유저의 모든 디바이스에서 로그아웃 (모든 RefreshToken 무효화)")
    @ApiResponse(responseCode = "204", description = "전체 로그아웃 성공")
    @PostMapping("/logout/all")
    public ResponseEntity<Void> logoutAll(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal) {
        authService.logoutAll(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
