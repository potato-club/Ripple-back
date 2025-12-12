package org.example.rippleback.features.feed.api.dto;

public record FeedReportResponseDto(
        Long feedId,
        Long reporterId,
        String message
) {}
