package org.example.rippleback.features.feed.api;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.security.jwt.JwtPrincipal;
import org.example.rippleback.features.feed.api.dto.FeedLikeResponseDto;
import org.example.rippleback.features.feed.api.dto.FeedRequestDto;
import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
import org.example.rippleback.features.feed.app.FeedLikeService;
import org.example.rippleback.features.feed.app.FeedService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final FeedLikeService feedLikeService;

    @PostMapping
    public FeedResponseDto createFeed(@AuthenticationPrincipal JwtPrincipal principal, @RequestBody FeedRequestDto request) {
        return feedService.createFeed(principal.userId(), request);
    }

    @GetMapping("/{id}")
    public FeedResponseDto getFeed(@PathVariable Long id) {
        return  feedService.getFeed(id);
    }

    @GetMapping
    public List<FeedResponseDto> getAllFeeds() {
        return feedService.getAllFeeds();
    }

    @PostMapping("/{id}/like")
    public FeedLikeResponseDto likeFeed(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal principal) {
        return feedLikeService.likeFeed(id, principal.userId());
    }

    @DeleteMapping("/{id}like")
    public FeedLikeResponseDto unlikeFeed(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal principal) {
        return feedLikeService.unlikeFeed(id, principal.userId());
    }
}
