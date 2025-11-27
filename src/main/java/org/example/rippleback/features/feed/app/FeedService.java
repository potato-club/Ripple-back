package org.example.rippleback.features.feed.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.feed.api.dto.FeedRequestDto;
import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedBookmark;
import org.example.rippleback.features.feed.domain.FeedLike;
import org.example.rippleback.features.feed.domain.FeedTag;
import org.example.rippleback.features.feed.infra.FeedBookmarkRepository;
import org.example.rippleback.features.feed.infra.FeedLikeRepository;
import org.example.rippleback.features.feed.infra.FeedRepository;
import org.example.rippleback.features.feed.infra.FeedTagRepository;
import org.example.rippleback.features.media.app.MediaUrlResolver;
import org.example.rippleback.features.user.infra.UserBlockRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedBookmarkRepository feedBookmarkRepository;
    private final FeedTagRepository tagRepository;
    private final FeedMapper feedMapper;
    private final MediaUrlResolver mediaUrlResolver;
    private final UserBlockRepository userBlockRepository;

    public FeedResponseDto createFeed(Long userId, FeedRequestDto request) {
        Feed feed = feedMapper.toEntity(userId, request);

        if (request.tags() != null) {
            List<FeedTag> tags = request.tags().stream()
                    .map(name -> tagRepository.findByName(name)
                            .orElseGet(() -> tagRepository.save(
                                    FeedTag.builder()
                                            .name(name)
                                            .build()
                            )))
                    .toList();

            feed.updateTags(tags);
        }

        feedRepository.save(feed);

        return feedMapper.toResponse(feed, mediaUrlResolver);
    }

    public FeedResponseDto getFeed(Long feedId, Long viewerId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾지 못했습니다."));
        Long authorId = feed.getAuthorId();

        boolean blocked = userBlockRepository.existsByBlockerIdAndBlockedId(authorId, viewerId);
        if (blocked) {
            throw new AccessDeniedException("차단관계입니다.");
        }

        return feedMapper.toResponse(feed, mediaUrlResolver);
    }

    public List<FeedResponseDto> getAllFeeds(Long viewerId) {
        List<Feed> feeds = feedRepository.findAllPublished();

        return feeds.stream()
                .filter(feed -> !userBlockRepository.existsByBlockerIdAndBlockedId(feed.getAuthorId(), viewerId))
                .map(feed -> feedMapper.toResponse(feed, mediaUrlResolver))
                .toList();
    }

    public void deleteFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾지 못했습니다."));

        if (!feed.getAuthorId().equals(userId))
            throw new SecurityException("다른 사람의 피드를 지울 수 없습니다.");

        feed.softDelete();
    }

    public void deleteAllByAuthorId(Long authorId) {
        List<Feed> feeds = feedRepository.findByAuthorId(authorId);

        feeds.forEach(Feed::softDelete);
    }

    public void likeFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId).orElseThrow();
        FeedLike like = FeedLike.builder()
                .feed(feed)
                .userId(userId)
                .build();
        feedLikeRepository.save(like);
        feed.increaseLikeCount();
    }

    public void bookmarkFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId).orElseThrow();
        FeedBookmark bookmark = FeedBookmark.builder()
                .feed(feed)
                .userId(userId)
                .build();
        feedBookmarkRepository.save(bookmark);
        feed.increaseBookmarkCount();
    }
}
