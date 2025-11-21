package org.example.rippleback.features.feed.api;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.security.jwt.JwtPrincipal;
import org.example.rippleback.features.feed.api.dto.FeedRequestDto;
import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
import org.example.rippleback.features.feed.app.FeedService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

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

    @PutMapping("/{feedId}")
    public FeedResponseDto updateFeed(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long feedId, @RequestBody FeedRequestDto request) {
        return feedService.updateFeed(principal.userId(), feedId, request);
    }

    @DeleteMapping("/{feedId}")
    public void deleteFeed(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long feedId) {
        feedService.deleteFeed(principal.userId(), feedId);
    }

    @PostMapping("/{feedId}/like")
    public void likeFeed(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long feedId) {
        feedService.likeFeed(principal.userId(), feedId);
    }

    @PostMapping("/{feedId}/bookmark")
    public void bookmarkFeed(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long feedId) {
        feedService.bookmarkFeed(principal.userId(), feedId);
    }
}
