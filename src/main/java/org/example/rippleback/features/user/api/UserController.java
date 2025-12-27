package org.example.rippleback.features.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Users", description = "회원/프로필/팔로우/차단 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "회원가입",
            description = """
                    이메일 인증 완료(verify)된 이메일로 사용자 계정을 생성합니다.
                    - username: 영문/숫자/밑줄 3~20자
                    - email: 인증 완료된 이메일
                    """
    )
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

    @Operation(
            summary = "이메일 인증 코드 전송",
            description = "사용자 이메일로 인증 코드를 발송합니다."
    )
    @ApiResponse(responseCode = "202", description = "이메일 전송 완료")
    @PostMapping("/email/verification/send")
    public ResponseEntity<Void> sendEmailCode(@Valid @RequestBody EmailVerificationSendRequestDto req) {
        emailVerificationService.sendCode(req.email());
        return ResponseEntity.accepted().build();
    }

    @Operation(
            summary = "이메일 인증 코드 검증",
            description = "사용자가 받은 코드로 이메일 인증을 완료합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "이메일 인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 코드/만료됨 (1002/1003)")
    })
    @PostMapping("/email/verification/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerificationVerifyRequestDto req) {
        emailVerificationService.verify(req.email(), req.code());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "내 정보 조회",
            description = "AccessToken 필요. 내 계정의 기본 정보를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "내 정보 반환")
    @GetMapping("/me")
    public ResponseEntity<MeResponseDto> me(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p
    ) {
        return ResponseEntity.ok(userService.getMe(p.userId()));
    }

    @Operation(
            summary = "프로필 조회 (ID)",
            description = "AccessToken 필요. 사용자 ID로 프로필을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 정보 반환"),
            @ApiResponse(responseCode = "404", description = "사용자 없음/삭제됨 (1005)")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfileById(id));
    }

    @Operation(
            summary = "프로필 조회 (username)",
            description = "AccessToken 필요. username으로 프로필을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 정보 반환"),
            @ApiResponse(responseCode = "404", description = "사용자 없음/삭제됨 (1005)")
    })
    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserResponseDto> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getProfileByUsername(username));
    }

    @Operation(
            summary = "유저 검색",
            description = """
                    query 기반 유저 검색(커서 페이징).
                    - cursor: 마지막 항목의 id(다음 페이지 조회용)
                    - size: 1~50 권장
                    """
    )
    @ApiResponse(responseCode = "200", description = "검색 결과 반환")
    @GetMapping
    public ResponseEntity<PageCursorResponse<UserSummaryDto>> search(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        return ResponseEntity.ok(userService.search(query, cursor, size));
    }

    @Operation(
            summary = "프로필 이미지 업로드용 presign 발급",
            description = """
                    AccessToken 필요.
                    1) mimeType/sizeBytes로 presigned PUT URL 발급
                    2) 클라이언트가 uploadUrl로 직접 업로드
                    3) 응답의 objectKey를 updateProfile(action=SET)로 전달하여 프로필 반영
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "presigned URL 발급 성공"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 mimeType/최대 용량 초과 등")
    })
    @PostMapping("/me/profile-image/presign")
    public ResponseEntity<ProfileImagePresignResponseDto> presignProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @Valid @RequestBody ProfileImagePresignRequestDto req
    ) {
        var res = userService.prepareProfileImageUpload(p.userId(), req);
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "프로필 수정",
            description = """
                    AccessToken 필요.
                    - username: 변경할 유저네임
                    - profileImage:
                      - action=KEEP  : 프로필 이미지 변경 없음(기본값)
                      - action=CLEAR : 프로필 이미지 제거
                      - action=SET   : objectKey로 프로필 이미지 교체
                        * objectKey는 반드시 users/{meId}/ 로 시작해야 함
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 objectKey/중복 username 등 (1601/1000)")
    })
    @PatchMapping("/me/profile")
    public ResponseEntity<UserResponseDto> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @Valid @RequestBody UpdateProfileRequestDto req
    ) {
        return ResponseEntity.ok(userService.updateProfile(p.userId(), req));
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "AccessToken 필요. 현재 비밀번호 검증 후 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 (1100)")
    })
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @Valid @RequestBody ChangePasswordRequestDto req
    ) {
        userService.changePassword(p.userId(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "AccessToken 필요. 계정 soft-delete 및 토큰 무효화 처리합니다."
    )
    @ApiResponse(responseCode = "204", description = "탈퇴 성공")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p
    ) {
        userService.softDelete(p.userId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "팔로우",
            description = "AccessToken 필요. targetId 사용자를 팔로우합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팔로우 성공"),
            @ApiResponse(responseCode = "409", description = "이미 팔로우한 상태 등")
    })
    @PutMapping("/me/followings/{targetId}")
    public ResponseEntity<FollowResponseDto> follow(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId
    ) {
        return ResponseEntity.ok(userService.follow(p.userId(), targetId));
    }

    @Operation(
            summary = "언팔로우",
            description = "AccessToken 필요. targetId 사용자를 언팔로우합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "언팔로우 성공(또는 이미 언팔 상태여도 멱등 삭제로 처리 가능)")
    })
    @DeleteMapping("/me/followings/{targetId}")
    public ResponseEntity<Void> unfollow(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId
    ) {
        userService.unfollow(p.userId(), targetId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "차단",
            description = "AccessToken 필요. targetId 사용자를 차단합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "차단 성공"),
            @ApiResponse(responseCode = "409", description = "이미 내가 차단한 상태")
    })
    @PutMapping("/me/blocks/{targetId}")
    public ResponseEntity<BlockResponseDto> block(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId
    ) {
        return ResponseEntity.ok(userService.block(p.userId(), targetId));
    }

    @Operation(
            summary = "차단 해제",
            description = "AccessToken 필요. 내가 차단한 targetId 사용자를 차단 해제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "차단 해제 성공(없어도 삭제는 멱등으로 처리 가능)")
    })
    @DeleteMapping("/me/blocks/{targetId}")
    public ResponseEntity<Void> unblock(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @PathVariable Long targetId
    ) {
        userService.unblock(p.userId(), targetId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "내 차단 목록 조회",
            description = """
                AccessToken 필요. 내가 차단한 사용자 목록을 커서 페이징으로 조회합니다.
                - cursor: 마지막 항목의 id(다음 페이지 조회용)
                - size: 1~50 권장
                """
    )
    @ApiResponse(responseCode = "200", description = "차단 목록 반환")
    @GetMapping("/me/blocks")
    public ResponseEntity<PageCursorResponse<UserSummaryDto>> listMyBlocks(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        return ResponseEntity.ok(userService.listMyBlocks(p.userId(), cursor, size));
    }
}
