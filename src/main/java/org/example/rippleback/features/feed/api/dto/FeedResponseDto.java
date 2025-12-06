package org.example.rippleback.features.feed.api.dto;

import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedStatus;
import org.example.rippleback.features.feed.domain.FeedVisibility;
import org.example.rippleback.features.user.api.dto.UserSummaryDto;

import java.time.Instant;
import java.util.List;

public record FeedResponseDto(
        Long id,
        UserSummaryDto author,
        String content,
        String[] tags,
        int likeCount,
        int bookmarkCount,
        int commentCount,
        int viewCount,
        String thumbnail,
        List<String> mediaUrls,
        FeedStatus feedStatus,
        FeedVisibility visibility,
        Instant createdAt,
        Instant updatedAt
) {
    public static FeedResponseDto from(Feed feed, UserSummaryDto summary) {
        return new FeedResponseDto(
                feed.getId(),
                summary,
                feed.getContent(),
                feed.getTagsNorm(),
                feed.getLikeCount(),
                feed.getBookmarkCount(),
                feed.getCommentCount(),
                feed.getViewCount(),
                feed.getThumbnail(),
                feed.getMediaKeys(),
                feed.getStatus(),
                feed.getVisibility(),
                feed.getCreatedAt(),
                feed.getUpdatedAt()
        );
    }
}
