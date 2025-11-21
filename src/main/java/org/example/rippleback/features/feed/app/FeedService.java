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

    public FeedResponseDto createFeed(Long userId, FeedRequestDto request) {
        Feed feed = feedMapper.toEntity(userId, request);
        feedRepository.save(feed);

        return feedMapper.toResponse(feed, mediaUrlResolver);
    }

    public FeedResponseDto getFeed(Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾지 못했습니다."));

        return feedMapper.toResponse(feed, mediaUrlResolver);
    }

    public List<FeedResponseDto> getAllFeeds() {
        return feedRepository.findAll().stream()
                .map(feed -> feedMapper.toResponse(feed, mediaUrlResolver))
                .toList();
    }

    public FeedResponseDto updateFeed(Long userId, Long feedId, FeedRequestDto request) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾지 못했습니다."));

        if (!feed.getAuthorId().equals(userId))
            throw new SecurityException("다른 사람의 피드를 변경할 수 없습니다.");

        feed.updateContent(request.content());
        feed.updateMediaKeys(request.mediaKeys());
        if (request.tags() != null) {
            List<FeedTag> tagEntities = request.tags()
                    .stream()
                    .map(name -> tagRepository.findByName(name)
                            .orElseGet(() -> FeedTag.builder()
                                    .name(name)
                                    .build()))
                    .toList();

            feed.updateTags(tagEntities);
        }
        feed.updateVisibility(request.visibility() != null ? request.visibility() : feed.getVisibility());

        return feedMapper.toResponse(feed, mediaUrlResolver);
    }

    public void deleteFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾지 못했습니다."));

        if (!feed.getAuthorId().equals(userId))
            throw new SecurityException("다른 사람의 피드를 지울 수 없습니다.");

        feed.softDelete();
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
