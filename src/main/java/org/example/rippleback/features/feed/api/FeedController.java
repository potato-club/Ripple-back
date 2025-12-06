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

    @PostMapping("/api/feeds")
    public FeedResponseDto createFeed(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody FeedRequestDto request
    ) {
        return feedService.createFeed(principal.userId(), request);
    }

    @DeleteMapping("/feeds/{feedId}")
    public void deleteFeed(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long feedId
    ) {
        feedService.deleteFeed(principal.userId(), feedId);
    }

    @GetMapping("/feeds/{id}")
    public FeedResponseDto getFeed(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id
    ) {
        return feedService.getFeed(id, principal.userId());
    }

    @GetMapping("/feeds/viewed")
    public List<FeedResponseDto> getUserAllFeeds(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return feedService.getUserAllFeeds(principal.userId());
    }

    @GetMapping("/feeds/home")
    public FeedPageDto getHomeFeeds(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int limit
    ){
        return feedService.getHomeFeeds(principal.userId(), cursor, limit);
    }

    @GetMapping("/{feedId}/fullView")
    public ResponseEntity<FeedFullViewDto> getFeedFullView(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long feedId
    ){
        FeedFullViewDto response = feedService.getFeedFullView(feedId, principal.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{feedId}/addLikes")
    public void addLike(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long feedId
    ) {
        feedService.addLike(principal.userId(), feedId);
    }

    @PostMapping("/{feedId}/removeLikes")
    public void removeLike(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long feedId
    ) {
        feedService.removeLike(principal.userId(), feedId);
    }

    @PostMapping("/{feedId}/addBookmarks")
    public void addBookmark(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long feedId
    ) {
        feedService.addBookmark(principal.userId(), feedId);
    }

    @PostMapping("/{feedId}/removeBookmarks")
    public void removeBookmark(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long feedId
    ) {
        feedService.addBookmark(principal.userId(), feedId);
    }

    @GetMapping("/search/tag")
    public List<String> searchTags(
            @RequestParam String query
    ) {
        return feedService.searchTags(query);
    }

    @GetMapping("/tag/{tagName}")
    public List<FeedResponseDto> getFeedsByTag(
            @PathVariable String tagName
    ) {
        return feedService.getFeedsByTag(tagName);
    }
}
