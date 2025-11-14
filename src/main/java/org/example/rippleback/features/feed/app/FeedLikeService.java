//package org.example.rippleback.features.feed.app;
//
//import lombok.RequiredArgsConstructor;
//import org.example.rippleback.features.feed.api.dto.FeedLikeResponseDto;
//import org.example.rippleback.features.feed.domain.Feed;
//import org.example.rippleback.features.feed.domain.FeedLike;
//import org.example.rippleback.features.feed.infra.FeedLikeRepository;
//import org.example.rippleback.features.feed.infra.FeedRepository;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class FeedLikeService {
//
//    private final FeedRepository feedRepository;
//    private final FeedLikeRepository feedLikeRepository;
//
//    @Transactional
//    public FeedLikeResponseDto likeFeed(Long feedId, Long userId) {
//        Feed feed = feedRepository.findById(feedId)
//                .orElseThrow(() -> new IllegalArgumentException("피드를 찾지 못했습니다."));
//
//        boolean alreadyLiked = feedLikeRepository.existsByFeedIdAndUserId(feedId, userId);
//
//        if (alreadyLiked) {
//           throw new IllegalArgumentException("이미 좋아요를 누른 게시물 입니다.");
//        }
//
//        feed.increaseLikeCount();
//        feedLikeRepository.save(FeedLike.builder()
//                .feed(feed)
//                .userId(userId)
//                .build()
//        );
//
//        return new FeedLikeResponseDto(feedId, feed.getLikeCount(), true);
//    }
//
//    @Transactional
//    public FeedLikeResponseDto unlikeFeed(Long feedId, Long userId) {
//        FeedLike like = feedLikeRepository.findByFeedIdAndUserId(feedId, userId)
//                .orElseThrow(() -> new IllegalStateException("좋아요를 누르지 않았습니다."));
//
//        Feed feed = like.getFeed();
//        feed.decreaseLikeCount();
//        feedLikeRepository.delete(like);
//
//        return new FeedLikeResponseDto(feedId, feed.getLikeCount(), false);
//    }
//}
