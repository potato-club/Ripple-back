package org.example.rippleback.features.feed.api;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.common.dto.ApiResponse;
import org.example.rippleback.core.security.jwt.JwtPrincipal;
import org.example.rippleback.features.feed.api.dto.*;
import org.example.rippleback.features.feed.app.FeedService;
import org.example.rippleback.features.user.app.CustomUserDetails;
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
    public FeedResponseDto createFeed(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody FeedRequestDto request
    ) {
        return feedService.createFeed(principal.userId(), request);
    }

    @PostMapping("/images/presign")
    public FeedImagePresignResponseDto presignFeedImages(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody FeedImagePresignRequestDto request
    ) {
        return feedService.prepareFeedImageUploads(principal.userId(), request);
    }

    @PostMapping("/videos/presign")
    public FeedVideoPresignResponseDto prepareFeedVideoUploads(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody FeedVideoPresignRequestDto request
    ) {
        return feedService.prepareFeedVideoUploads(principal.userId(), request);
    }

    @PatchMapping("/{feedId}/visibility")
    public ApiResponse<Void> changeVisibility(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long feedId,
            @RequestBody ChangeVisibilityRequestDto request
    ) {
        feedService.changeVisibility(principal.userId(), feedId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{feedId}")
    public void deleteFeed(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long feedId
    ) {
        feedService.deleteFeed(principal.userId(), feedId);
    }

    @GetMapping("/{feedId}")
    public FeedResponseDto getFeed(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long feedId
    ) {
        return feedService.getFeed(feedId, principal.userId());
    }

    @GetMapping("/viewed")
    public List<FeedResponseDto> getUserAllFeeds(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return feedService.getUserAllFeeds(principal.userId());
    }

    @GetMapping("/home")
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
        return ResponseEntity.ok(feedService.getFeedFullView(principal.userId(), feedId));
    }

    @PostMapping("/{feedId}/report")
    public ApiResponse<FeedReportResponseDto> reportFeed(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long feedId,
            @RequestBody FeedReportRequestDto request
    ) {
        return ApiResponse.ok(feedService.reportFeed(feedId, principal.userId(), request));
    }

    @PostMapping("/{feedId}/likes")
    public void addLike(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long feedId) {
        feedService.addLike(principal.userId(), feedId);
    }

    @DeleteMapping("/{feedId}/likes")
    public void removeLike(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long feedId) {
        feedService.removeLike(principal.userId(), feedId);
    }

    @PostMapping("/{feedId}/bookmarks")
    public void addBookmark(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long feedId) {
        feedService.addBookmark(principal.userId(), feedId);
    }

    @DeleteMapping("/{feedId}/bookmarks")
    public void removeBookmark(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long feedId) {
        feedService.removeBookmark(principal.userId(), feedId);
    }

    @GetMapping("/search/tag")
    public List<String> searchTags(@RequestParam String query) {
        return feedService.searchTags(query);
    }

    @GetMapping("/tag/{tagName}")
    public List<FeedResponseDto> getFeedsByTag(@Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal p,
                                               @PathVariable String tagName
    ) {
        return feedService.getFeedsByTag(p.userId(), tagName);
    }
}
