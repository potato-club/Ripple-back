package org.example.rippleback.features.comment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        Long parentId,
        @NotBlank
        @Size(max = 200)
        String content
) {
}