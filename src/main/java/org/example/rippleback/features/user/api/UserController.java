package org.example.rippleback.features.user.api;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.security.jwt.JwtPrincipal;
import org.example.rippleback.features.user.api.dto.*;
import org.example.rippleback.features.user.app.EmailVerificationService;
import org.example.rippleback.features.user.app.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping
    public ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody SignupRequestDto req) {
        var res = userService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/email/verification/send")
    public ResponseEntity<Void> sendEmailCode(@Valid @RequestBody EmailVerificationSendRequestDto req) {
        emailVerificationService.sendCode(req.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/email/verification/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerificationVerifyRequestDto req) {
        emailVerificationService.verify(req.email(), req.code());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDto> me(@Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p) {
        return ResponseEntity.ok(userService.getMe(p.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfileById(id));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserResponseDto> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getProfileByUsername(username));
    }

    @GetMapping
    public ResponseEntity<PageCursorResponse<UserSummaryDto>> search(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        return ResponseEntity.ok(userService.search(query, cursor, size));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<UserResponseDto> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @Valid @RequestBody UpdateProfileRequestDto req) {
        return ResponseEntity.ok(userService.updateProfile(p.userId(), req));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @Valid @RequestBody ChangePasswordRequestDto req) {
        userService.changePassword(p.userId(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p) {
        userService.softDelete(p.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{targetId}/follow")
    public ResponseEntity<FollowResponseDto> follow(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId) {
        var res = userService.follow(p.userId(), targetId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @DeleteMapping("/{targetId}/follow")
    public ResponseEntity<Void> unfollow(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId) {
        userService.unfollow(p.userId(), targetId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{targetId}/block")
    public ResponseEntity<BlockResponseDto> block(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId) {
        var res = userService.block(p.userId(), targetId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @DeleteMapping("/{targetId}/block")
    public ResponseEntity<Void> unblock(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId) {
        userService.unblock(p.userId(), targetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/followers")
    public ResponseEntity<PageCursorResponse<UserSummaryDto>> followers(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(userService.listFollowers(id, cursor, size));
    }

    @GetMapping("/{id}/followings")
    public ResponseEntity<PageCursorResponse<UserSummaryDto>> followings(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(userService.listFollowings(id, cursor, size));
    }

    @GetMapping("/me/blocks")
    public ResponseEntity<PageCursorResponse<UserSummaryDto>> myBlocks(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(userService.listMyBlocks(p.userId(), cursor, size));
    }

    @GetMapping("/availability")
    public ResponseEntity<UserService.AvailabilityResponse> availability(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(userService.availability(username, email));
    }
}
