package org.example.rippleback.features.comment.api.dto;

import java.util.List;

public record CommentPageResponseDto(
        List<CommentResponseDto> comments,
        Long nextCursor,
        boolean hasNext
) { }
