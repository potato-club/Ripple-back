package org.example.rippleback.features.feed.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.feed.api.dto.FeedRequestDto;
import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedStatus;
import org.example.rippleback.features.feed.domain.FeedVisibility;
import org.example.rippleback.features.feed.domain.FeedTag;
import org.example.rippleback.features.feed.infra.FeedTagRepository;
import org.example.rippleback.features.media.app.MediaUrlResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FeedMapper {

    private final FeedTagRepository tagRepository;

    public Feed toEntity(Long userId, FeedRequestDto request) {
        Feed feed = Feed.builder()
                .authorId(userId)
                .content(request.content())
                .mediaKeys(request.mediaKeys())
                .visibility(request.visibility() == null ? FeedVisibility.PUBLIC : request.visibility())
                .status(FeedStatus.PUBLISHED)
                .build();

        if (request.tags() != null) {
            feed.updateTags(
                    request.tags().stream()
                            .map(name -> tagRepository.findByName(name)
                                    .orElseGet(() -> FeedTag.builder().name(name)
                                            .build()))
                            .toList()
            );
        }

        return feed;
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
