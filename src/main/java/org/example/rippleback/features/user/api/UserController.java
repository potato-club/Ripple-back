package org.example.rippleback.features.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "회원가입", description = "이메일 인증 완료 후 사용자 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "403", description = "이메일 미인증 (1004)"),
            @ApiResponse(responseCode = "400", description = "검증 오류 또는 중복 (1000/1001/9000)")
    })
    @PostMapping
    public ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody SignupRequestDto req) {
        var res = userService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @Operation(summary = "이메일 인증 코드 전송", description = "인증을 위한 이메일 코드 전송")
    @ApiResponse(responseCode = "202", description = "이메일 전송 완료")
    @PostMapping("/email/verification/send")
    public ResponseEntity<Void> sendEmailCode(@Valid @RequestBody EmailVerificationSendRequestDto req) {
        emailVerificationService.sendCode(req.email());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "이메일 인증 코드 검증", description = "사용자가 받은 코드를 통해 이메일 인증")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "이메일 인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 코드/만료됨 (1002/1003)")
    })
    @PostMapping("/email/verification/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerificationVerifyRequestDto req) {
        emailVerificationService.verify(req.email(), req.code());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 정보 조회", description = "AccessToken 필요")
    @ApiResponse(responseCode = "200", description = "내 정보 반환")
    @GetMapping("/me")
    public ResponseEntity<MeResponseDto> me(@Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p) {
        return ResponseEntity.ok(userService.getMe(p.userId()));
    }

    @Operation(summary = "프로필 조회 (ID)", description = "AccessToken 필요")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 정보 반환"),
            @ApiResponse(responseCode = "404", description = "사용자 없음/삭제됨 (1005)")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfileById(id));
    }

    @Operation(summary = "프로필 조회 (유저네임)", description = "AccessToken 필요")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 정보 반환"),
            @ApiResponse(responseCode = "404", description = "사용자 없음/삭제됨 (1005)")
    })
    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserResponseDto> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getProfileByUsername(username));
    }

    @Operation(summary = "유저 검색", description = "쿼리 기반 유저 검색")
    @ApiResponse(responseCode = "200", description = "검색 결과 반환")
    @GetMapping
    public ResponseEntity<PageCursorResponse<UserSummaryDto>> search(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(userService.search(query, cursor, size));
    }

    @Operation(summary = "프로필 수정", description = "AccessToken 필요")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @ApiResponse(responseCode = "400", description = "이미지 URL 오류/중복 유저네임 (1603/1000)")
    })
    @PatchMapping("/me/profile")
    public ResponseEntity<UserResponseDto> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @Valid @RequestBody UpdateProfileRequestDto req) {
        return ResponseEntity.ok(userService.updateProfile(p.userId(), req));
    }

    @Operation(summary = "비밀번호 변경", description = "AccessToken 필요")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 (1100)")
    })
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @Valid @RequestBody ChangePasswordRequestDto req) {
        userService.changePassword(p.userId(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴", description = "AccessToken 필요")
    @ApiResponse(responseCode = "204", description = "탈퇴 성공")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p) {
        userService.softDelete(p.userId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "팔로우", description = "AccessToken 필요")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "팔로우 성공"),
            @ApiResponse(responseCode = "400", description = "자기 자신 팔로우/중복/차단 (1015/1011/1017)"),
            @ApiResponse(responseCode = "404", description = "사용자 없음 (1005)")
    })
    @PostMapping("/{targetId}/follow")
    public ResponseEntity<FollowResponseDto> follow(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId) {
        var res = userService.follow(p.userId(), targetId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @Operation(summary = "언팔로우", description = "AccessToken 필요")
    @ApiResponse(responseCode = "204", description = "언팔로우 성공 (멱등)")
    @DeleteMapping("/{targetId}/follow")
    public ResponseEntity<Void> unfollow(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId) {
        userService.unfollow(p.userId(), targetId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "차단", description = "AccessToken 필요")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "차단 성공"),
            @ApiResponse(responseCode = "400", description = "자기 자신 차단/중복 차단 (1016/1013)"),
            @ApiResponse(responseCode = "404", description = "사용자 없음 (1005)")
    })
    @PostMapping("/{targetId}/block")
    public ResponseEntity<BlockResponseDto> block(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId) {
        var res = userService.block(p.userId(), targetId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @Operation(summary = "차단 해제", description = "AccessToken 필요")
    @ApiResponse(responseCode = "204", description = "차단 해제 성공 (멱등)")
    @DeleteMapping("/{targetId}/block")
    public ResponseEntity<Void> unblock(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId) {
        userService.unblock(p.userId(), targetId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "팔로워 목록 조회", description = "AccessToken 필요")
    @ApiResponse(responseCode = "200", description = "팔로워 목록 반환")
    @GetMapping("/{id}/followers")
    public ResponseEntity<PageCursorResponse<UserSummaryDto>> followers(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(userService.listFollowers(id, cursor, size));
    }

    @Operation(summary = "팔로잉 목록 조회", description = "AccessToken 필요")
    @ApiResponse(responseCode = "200", description = "팔로잉 목록 반환")
    @GetMapping("/{id}/followings")
    public ResponseEntity<PageCursorResponse<UserSummaryDto>> followings(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(userService.listFollowings(id, cursor, size));
    }

    @Operation(summary = "내 차단 목록 조회", description = "AccessToken 필요")
    @ApiResponse(responseCode = "200", description = "차단 목록 반환")
    @GetMapping("/me/blocks")
    public ResponseEntity<PageCursorResponse<UserSummaryDto>> myBlocks(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(userService.listMyBlocks(p.userId(), cursor, size));
    }

    @Operation(summary = "유저 중복 확인", description = "username/email 중복 확인")
    @ApiResponse(responseCode = "200", description = "중복 여부 반환")
    @GetMapping("/availability")
    public ResponseEntity<UserService.AvailabilityResponse> availability(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(userService.availability(username, email));
    }
}
