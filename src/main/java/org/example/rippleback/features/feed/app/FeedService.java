package org.example.rippleback.features.feed.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import org.example.rippleback.features.feed.api.dto.FeedPageDto;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        Long authorId = feed.getAuthorId();

        boolean blocked = userBlockRepository.existsByBlockerIdAndBlockedId(authorId, viewerId);
        if (blocked) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return feedMapper.toResponse(feed, mediaUrlResolver);
    }

    public List<FeedResponseDto> getUserAllFeeds(Long viewerId) {
        List<Feed> feeds = feedRepository.findAllPublished();

        return feeds.stream()
                .filter(feed -> !userBlockRepository.existsByBlockerIdAndBlockedId(feed.getAuthorId(), viewerId))
                .map(feed -> feedMapper.toResponse(feed, mediaUrlResolver))
                .toList();
    }

    public FeedPageDto getHomeFeeds(Long cursor, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Feed> feeds = feedRepository.findFeedsForHome(cursor, pageable);
        Long nextCursor = feeds.isEmpty()
                ? null
                : feeds.getLast().getId();

        return new FeedPageDto(
                feeds.stream()
                        .map(feed -> feedMapper.toResponse(feed, mediaUrlResolver))
                        .toList(),
                nextCursor,
                feeds.size() == limit
        );

    }

    public void deleteFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (!feed.getAuthorId().equals(userId))
            throw new BusinessException(ErrorCode.INVALID_DELETE_OTHER);

        feed.softDelete();
    }

    public void deleteAllByAuthorId(Long authorId) {
        List<Feed> feeds = feedRepository.findByAuthorId(authorId);

        feeds.forEach(Feed::softDelete);
    }

    public void likeFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        Optional<FeedLike> on = feedLikeRepository.findByFeedIdAndUserId(feedId, userId);

        if (on.isPresent()) {
            feedLikeRepository.delete(on.get());
            feed.decreaseLikeCount();
            return;
        }

        FeedLike like = FeedLike.builder()
                .feed(feed)
                .userId(userId)
                .createdAt(Instant.now())
                .build();

        feedLikeRepository.save(like);
        feed.increaseLikeCount();
        feedRepository.save(feed);
    }

    public void bookmarkFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        Optional<FeedBookmark> on = feedBookmarkRepository.findByFeedIdAndUserId(feedId, userId);

        if (on.isPresent()) {
            feedBookmarkRepository.delete(on.get());
            feed.decreaseBookmarkCount();
            return;
        }

        FeedBookmark bookmark = FeedBookmark.builder()
                .feed(feed)
                .userId(userId)
                .createdAt(Instant.now())
                .build();

        feedBookmarkRepository.save(bookmark);
        feed.increaseBookmarkCount();
        feedRepository.save(feed);
    }

    public void commentFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
    }
}
