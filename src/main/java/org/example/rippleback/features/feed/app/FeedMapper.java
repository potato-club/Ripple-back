//package org.example.rippleback.features.feed.app;
//
//import org.example.rippleback.features.feed.api.dto.FeedRequestDto;
//import org.example.rippleback.features.feed.api.dto.FeedResponseDto;
//import org.example.rippleback.features.feed.domain.Feed;
//import org.example.rippleback.features.media.app.MediaUrlResolver;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Component
//public class FeedMapper {
//
//    public Feed toEntity(Long userId, FeedRequestDto request) {
//        return Feed.builder()
//                .userId(userId)
//                .content(request.content())
//                .mediaKeys(request.mediaKeys())
//                .build();
//    }
//
//    public FeedResponseDto toResponse(Feed feed, MediaUrlResolver resolver) {
//        List<String> urls = feed.getMediaKeys() == null ? List.of()
//                : feed.getMediaKeys()
//                .stream()
//                .map(resolver::toPublicUrl)
//                .toList();
//
//        return new FeedResponseDto(feed.getId(), feed.getUserId(), feed.getContent(), feed.getLikeCount(), urls);
//    }
//}
