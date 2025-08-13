package org.example.rippleback.features.user.api.dto;

import java.util.List;

public record PageCursorResponse<T>(
        List<T> items,
        String nextCursor,   // null 이면 마지막 페이지
        boolean hasNext
) {}