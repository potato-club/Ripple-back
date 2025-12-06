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
    private final FeedTagRelationRepository feedTagRelationRepository;


    public FeedResponseDto createFeed(Long userId, FeedRequestDto request) {
        Feed feed = feedMapper.toEntity(userId, request);
        feedRepository.save(feed);

        if (request.tags() != null) {
            List<String> norm = new ArrayList<>();

            for (String name : request.tags()) {
                // 1. 태그 정규화
                String tagName = name.toLowerCase().trim();
                norm.add(tagName);

                // 2. 태그 엔티티 조회 or 생성
                FeedTag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(
                                FeedTag.builder()
                                        .name(tagName)
                                        .build()
                        ));

                // 3. 테이블 저장
                feedTagRelationRepository.save(
                        FeedTagRelation.builder()
                                .feedId(feed.getId())
                                .tagId(tag.getId())
                                .build()
                );

                // 4. Feed 도메인 메서드로 tagsNorm 설정
                feed.updateTagsNorm(norm);
            }
        }

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
                .filter(feed -> feedViewHistoryRepository.existsByUserIdAndFeedId(viewerId, feed.getId()))
                .map(feed -> feedMapper.toResponse(feed, mediaUrlResolver))
                .toList();
    }


    public FeedPageDto getHomeFeeds(Long viewerId, Long cursor, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Feed> feeds = feedRepository.findFeedsForHome(cursor, pageable);
        List<Feed> filtered = feeds.stream()
                .filter(feed -> !userBlockRepository.existsAnyBlock(viewerId, feed.getAuthorId()))
                .toList();

        boolean hasNext = false;

        if (filtered.size() > limit) {
            hasNext = true;
            filtered = filtered.subList(0, limit);
        }

        Long nextCursor = feeds.isEmpty() ? null : filtered.getLast().getId();

        return new FeedPageDto(
                filtered.stream()
                        .map(feed -> feedMapper.toResponse(feed, mediaUrlResolver))
                        .toList(),
                nextCursor,
                hasNext
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
                videoHlsUrl = mediaUrlResolver.hlsManifestUrl(key); // 영상의 경우: key = "videos/abc123"
                videoSourceUrl = mediaUrlResolver.videoSourceUrl(key, "mp4"); // 규칙에 따라 mp4 또는 다른 확장자
            }

            else {
                // 이미지의 경우 바로 공개 URL
                imageUrls.add(mediaUrlResolver.toPublicUrl(key));
            }
        }

        List<String> tagNames = feedTagRelationRepository.findByFeedId(feedId)
                .stream()
                .map(rel -> tagRepository.findById(rel.getTagId()).orElseThrow())
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


    public void addLike(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (feedLikeRepository.existsByFeedIdAndUserId(feedId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED_FEED);
        }

        FeedLike like = FeedLike.create(feed, userId);
        feedLikeRepository.save(like);
        feedRepository.incrementLikeCount(feedId);
    }


    public void removeLike(Long userId, Long feedId) {
        int updated = feedRepository.decrementLikeCount(feedId);

        if (updated == 0) {
            throw new BusinessException(ErrorCode.INVALID_LIKE_STATE);
        }

        feedLikeRepository.deleteByUserIdAndFeedId(userId, feedId);
        feedRepository.decrementLikeCount(feedId);
    }


    public void addBookmark(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (feedBookmarkRepository.existsByFeedIdAndUserId(feedId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_BOOKMARKED);
        }

        FeedBookmark bookmark = FeedBookmark.create(feed, userId);
        feedBookmarkRepository.save(bookmark);
        feedRepository.incrementBookmarkCount(feedId);
    }


    public void removeBookmark(Long userId, Long feedId) {
        int updated = feedRepository.decrementBookmarkCount(feedId);

        if (updated == 0) {
            throw new BusinessException(ErrorCode.INVALID_BOOKMARK_STATE);
        }

        feedBookmarkRepository.deleteByUserIdAndFeedId(userId, feedId);
        feedRepository.decrementBookmarkCount(feedId);
    }


    public List<String> searchTags(String keyword) {
        return tagRepository.searchByKeyword(keyword.toLowerCase().trim())
                .stream()
                .map(FeedTag::getName)
                .toList();
    }


    public List<FeedResponseDto> getFeedsByTag(String tagName) {
        FeedTag tag = tagRepository.findByName(tagName.toLowerCase().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));

        List<Long> feedIds = feedTagRelationRepository.findFeedIdsByTagId(tag.getId());
        List<Feed> feeds = feedRepository.findByIdIn(feedIds);

        return feeds.stream()
                .map(feed -> feedMapper.toResponse(feed, mediaUrlResolver))
                .toList();
    }

}
