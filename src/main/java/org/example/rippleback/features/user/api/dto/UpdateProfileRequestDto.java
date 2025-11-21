package org.example.rippleback.features.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "프로필 수정 요청(닉네임/메시지/이미지 SET|CLEAR|KEEP)")
public record UpdateProfileRequestDto(
        @Schema(example = "neo_21")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "영문/숫자/밑줄 3~20자")
        String username,

        @Schema(description = "프로필 이미지 패치 (null이면 KEEP과 동일)")
        UpdateProfileRequestDto.ProfileImagePatch profileImage
) {
    @Schema(description = "이미지 변경 액션")
    public static record ProfileImagePatch(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "SET",
                    description = "SET|CLEAR|KEEP")
            Action action,

            @Schema(example = "12345", description = "action=SET일 때만 필요")
            Long mediaId
    ) {
        public enum Action {SET, CLEAR, KEEP}
    }
}