package org.example.rippleback.features.comment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequestDto(
        Long parentId,

        @NotBlank
        @Size(max = 300)
        String content
) {
}