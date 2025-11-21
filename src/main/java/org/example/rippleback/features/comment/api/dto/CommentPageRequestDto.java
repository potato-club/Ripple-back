package org.example.rippleback.features.comment.api.dto;

public record CommentPageRequestDto(
        Long cursorId,
        Integer size
) {
}
