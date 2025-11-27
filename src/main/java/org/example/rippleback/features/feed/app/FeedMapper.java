package org.example.rippleback.features.feed.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.feed.api.dto.FeedRequestDto;
import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedStatus;
import org.example.rippleback.features.feed.domain.FeedVisibility;
import org.example.rippleback.features.media.app.MediaUrlResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FeedMapper {

    public Feed toEntity(Long userId, FeedRequestDto request) {
        return Feed.builder()
                .authorId(userId)
                .content(request.content())
                .mediaKeys(request.mediaKeys())
                .visibility(request.visibility() == null ? FeedVisibility.PUBLIC : request.visibility())
                .status(FeedStatus.PUBLISHED)
                .build();
    }


    public FeedResponseDto toResponse(Feed feed, MediaUrlResolver resolver) {
        List<String> urls = feed.getMediaKeys() == null ? List.of()
                : feed.getMediaKeys()
                .stream()
                .map(resolver::toPublicUrl)
                .toList();

        return new FeedResponseDto(
                feed.getId(),
                feed.getAuthorId(),
                feed.getContent(),
                urls,
                feed.getTagsNorm(),
                feed.getLikeCount(),
                feed.getBookmarkCount(),
                feed.getVisibility(),
                feed.getStatus(),
                feed.getCreatedAt(),
                feed.getUpdatedAt()
                );
    }
}
