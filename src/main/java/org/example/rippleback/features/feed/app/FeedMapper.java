package org.example.rippleback.features.feed.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.media.app.MediaUrlResolver;
import org.example.rippleback.features.media.domain.Media;
import org.example.rippleback.features.user.app.UserMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedMapper {

    private final UserMapper userMapper;

    public FeedResponseDto toResponse(Feed feed, MediaUrlResolver resolver, boolean isFollow) {
        String thumbnailUrl = null;

        Media tm = feed.getThumbnailMedia();
        if (tm != null) {
            String key = tm.getObjectKey();
            if (key != null && !key.isBlank()) {
                thumbnailUrl = resolver.toPublicUrl(key);
            }
        }

        return new FeedResponseDto(
                feed.getId(),
                userMapper.toSummary(feed.getAuthor(), isFollow),
                feed.getContent(),
                feed.getTagsNorm(),
                feed.getLikeCount(),
                feed.getBookmarkCount(),
                feed.getCommentCount(),
                feed.getViewCount(),
                thumbnailUrl,
                feed.getStatus(),
                feed.getVisibility(),
                feed.getCreatedAt()
        );
    }
}
