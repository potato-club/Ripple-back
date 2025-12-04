package org.example.rippleback.features.feed.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import org.example.rippleback.features.feed.api.dto.FeedFullViewDto;
import org.example.rippleback.features.feed.api.dto.FeedPageDto;
import org.example.rippleback.features.feed.api.dto.FeedRequestDto;
import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
import org.example.rippleback.features.feed.domain.*;
import org.example.rippleback.features.feed.infra.*;
import org.example.rippleback.features.media.app.MediaUrlResolver;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.features.user.infra.UserBlockRepository;
import org.example.rippleback.features.user.infra.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
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
    private final FeedViewHistoryRepository feedViewHistoryRepository;
    private final UserRepository userRepository;

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

    public FeedFullViewDto getFeedFullView(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        boolean hasViewed = feedViewHistoryRepository.existsByUserIdAndFeedId(userId, feedId);

        if (!hasViewed) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            if (user.getCredits() <= 0) {
                throw new BusinessException(ErrorCode.NOT_ENOUGH_CREDITS);
            }

            user.decreaseCredits(1);

            feedViewHistoryRepository.save(FeedViewHistory.builder()
                    .userId(userId)
                    .feedId(feedId)
                    .viewedAt(Instant.now())
                    .build()
            );
        }

        boolean liked = feedLikeRepository.existsByFeedIdAndUserId(feedId, userId);
        boolean bookmarked = feedBookmarkRepository.existsByFeedIdAndUserId(feedId, userId);

        String videoHlsUrl = null;
        String videoSourceUrl = null;
        List<String> imageUrls = new ArrayList<>();

        for (String key : feed.getMediaKeys()) {
            boolean isVideo = !key.contains(".") && !key.endsWith("/");

            if (isVideo) {
                // 영상의 경우: key = "videos/abc123"
                videoHlsUrl = mediaUrlResolver.hlsManifestUrl(key);
                videoSourceUrl = mediaUrlResolver.videoSourceUrl(key, "mp4"); // 규칙에 따라 mp4 또는 다른 확장자
            }

            else {
                // 이미지의 경우 바로 공개 URL
                imageUrls.add(mediaUrlResolver.toPublicUrl(key));
            }
        }

        List<String> tagNames = feed.getTags().stream()
                .map(FeedTag::getName)
                .toList();

        return FeedFullViewDto.builder()
                .id(feed.getId())
                .authorId(feed.getAuthorId())
                .authorName(feed.getAuthor().getUsername())
                .content(feed.getContent())
                .imageUrls(imageUrls)
                .videoHlsUrl(videoHlsUrl)
                .videoSourceUrl(videoSourceUrl)
                .tags(tagNames)
                .likeCount(feed.getLikeCount())
                .bookmarkCount(feed.getBookmarkCount())
                .viewCount(feed.getViewCount())
                .liked(liked)
                .bookmarked(bookmarked)
                .createdAt(feed.getCreatedAt())
                .build();

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
}
