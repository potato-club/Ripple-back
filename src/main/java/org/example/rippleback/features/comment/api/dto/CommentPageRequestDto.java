package org.example.rippleback.features.comment.api.dto;

import org.example.rippleback.features.comment.domain.CommentSortType;

public record CommentPageRequestDto(
        Long cursorId,
        Integer size,
        CommentSortType sort
) {
}
