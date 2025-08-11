package org.example.rippleback.features.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.auth.app.AuthService;
import org.example.rippleback.features.auth.api.dto.LoginRequestDto;
import org.example.rippleback.features.auth.api.dto.LoginResponseDto;
import org.example.rippleback.features.auth.api.dto.TokenRequestDto;
import org.example.rippleback.features.auth.api.dto.TokenResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@Valid @RequestBody TokenRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       @RequestHeader("X-Device-Id") String deviceId) {
        String bearer = request.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        String accessToken = bearer.substring(7);
        authService.logout(accessToken, deviceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout/all")
    public ResponseEntity<Void> logoutAll(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        String accessToken = bearer.substring(7);
        authService.logoutAll(accessToken);
        return ResponseEntity.noContent().build();
    }
}