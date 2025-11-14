package org.example.rippleback.features.feed.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.feed.api.dto.FeedRequestDto;
import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.infra.FeedRepository;
import org.example.rippleback.features.media.app.MediaUrlResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedMapper feedMapper;
    private final MediaUrlResolver mediaUrlResolver;

    @Transactional
    public FeedResponseDto createFeed(Long userId, FeedRequestDto request) {
        Feed feed = feedMapper.toEntity(userId, request);
        Feed savedFeed = feedRepository.save(feed);

        return feedMapper.toResponse(savedFeed, mediaUrlResolver);
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
}
