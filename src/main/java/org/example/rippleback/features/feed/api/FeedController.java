package org.example.rippleback.features.feed.api;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.security.jwt.JwtPrincipal;
import org.example.rippleback.features.feed.api.dto.FeedFullViewDto;
import org.example.rippleback.features.feed.api.dto.FeedPageDto;
import org.example.rippleback.features.feed.api.dto.FeedRequestDto;
import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
import org.example.rippleback.features.feed.app.FeedService;
import org.springframework.http.ResponseEntity;
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

    @DeleteMapping("/{feedId}")
    public void deleteFeed(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long feedId) {
        feedService.deleteFeed(principal.userId(), feedId);
    }

    @GetMapping("/{id}")
    public FeedResponseDto getFeed(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal principal) {
        return  feedService.getFeed(id, principal.userId());
    }

    @GetMapping
    public List<FeedResponseDto> getUserAllFeeds(@AuthenticationPrincipal JwtPrincipal principal) {
        return feedService.getUserAllFeeds(principal.userId());
    }

    @GetMapping
    public FeedPageDto getHomeFeeds(@RequestParam(required = false) Long cursor, @RequestParam(defaultValue = "10") int limit){
        return feedService.getHomeFeeds(cursor, limit);
    }

    @GetMapping("/{feedId}")
    public ResponseEntity<FeedFullViewDto> getFeedFullView(@PathVariable Long feedId, @AuthenticationPrincipal JwtPrincipal principal){
        FeedFullViewDto response = feedService.getFeedFullView(feedId, principal.userId());
        return ResponseEntity.ok(response);
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
