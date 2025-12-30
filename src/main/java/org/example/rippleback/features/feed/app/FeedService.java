package org.example.rippleback.features.feed.app;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import org.example.rippleback.features.feed.api.dto.*;
import org.example.rippleback.features.feed.domain.*;
import org.example.rippleback.features.feed.infra.*;
import org.example.rippleback.features.media.app.MediaUrlResolver;
import org.example.rippleback.features.media.domain.Media;
import org.example.rippleback.features.media.domain.MediaStatus;
import org.example.rippleback.features.media.domain.MediaType;
import org.example.rippleback.features.media.infra.MediaRepository;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.features.user.infra.UserBlockRepository;
import org.example.rippleback.features.user.infra.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedService {

    private static final int MAX_FEED_IMAGES = 15;
    private static final long MAX_FEED_IMAGE_BYTES = 5 * 1024 * 1024L; // 5MB
    private static final Duration FEED_IMAGE_PRESIGN_TTL = Duration.ofMinutes(10);

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
    private final FeedReportRepository feedReportRepository;

    private final FeedMediaRepository feedMediaRepository;
    private final MediaRepository mediaRepository;

    private final S3Presigner s3Presigner;
    private final Clock clock;
    private final EntityManager em;

    @Value("${media.bucket}")
    private String mediaBucket;

    /**
     * [NEW] 피드 이미지 업로드용 presigned PUT URL 발급 (최대 15장)
     * - 프론트는 uploadUrl로 S3에 직접 PUT 업로드
     * - 업로드 후 objectKey 리스트를 createFeed에 전달
     */
    @Transactional(readOnly = true)
    public FeedImagePresignResponseDto prepareFeedImageUploads(Long userId, FeedImagePresignRequestDto request) {
        if (request == null || request.files() == null || request.files().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }
        if (request.files().size() > MAX_FEED_IMAGES) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        List<FeedImagePresignResponseDto.Item> items = new ArrayList<>(request.files().size());

        for (FeedImagePresignRequestDto.FileSpec f : request.files()) {
            if (f == null) continue;

            long size = (f.sizeBytes() == null) ? -1L : f.sizeBytes();
            if (size <= 0 || size > MAX_FEED_IMAGE_BYTES) {
                // 프로젝트에 맞는 에러코드가 있으면 교체하세요 (예: MEDIA_SIZE_EXCEEDED)
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }

            String ext = mapImageExtFromMime(f.mimeType());
            if (ext == null) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }

            String objectKey = buildFeedImageKey(userId, ext);
            String uploadUrl = createPresignedPutUrl(objectKey, f.mimeType());

            items.add(new FeedImagePresignResponseDto.Item(
                    uploadUrl,
                    objectKey,
                    MAX_FEED_IMAGE_BYTES
            ));
        }

        return new FeedImagePresignResponseDto(items);
    }

    /**
     * [CHANGED] 피드 생성 (이미지형)
     * - request.imageObjectKeys(업로드 완료된 objectKey들) 기반
     * - Media 먼저 생성(FEED_IMAGE) -> 첫 Media.id를 thumbnailMediaId로 사용
     * - Feed 생성 -> FeedMedia 연결 생성(sortOrder)
     */
    public FeedResponseDto createFeed(Long userId, FeedRequestDto request) {

        // 1) tagsNorm 미리 생성 (Feed는 생성 후 수정 불가 정책)
        String[] tagsNormArr = normalizeTags(request.tags());

        // 2) 이미지 objectKey 리스트 확보 + 검증(최대 15장, 본인 prefix 강제, 확장자 검증)
        List<String> keys = request.imageObjectKeys() == null ? List.of() : request.imageObjectKeys();
        if (keys.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }
        if (keys.size() > MAX_FEED_IMAGES) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        String expectedPrefix = "users/" + userId + "/feeds/images/";
        for (String k : keys) {
            if (k == null || k.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }
            String key = k.trim();
            if (!key.startsWith(expectedPrefix)) {
                // 프로젝트에 맞는 에러코드가 있으면 교체하세요 (예: INVALID_OBJECT_KEY)
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }
            if (!hasAllowedImageExt(key)) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }
        }

        // 3) Media 먼저 생성 (ownerId는 userId로 통일)
        List<Media> medias = new ArrayList<>(keys.size());
        for (String key : keys) {
            medias.add(Media.builder()
                    .ownerId(userId)
                    .mediaType(MediaType.FEED_IMAGE)
                    .objectKey(key.trim())
                    .mediaStatus(MediaStatus.READY)
                    .build());
        }
        medias = mediaRepository.saveAll(medias);

        Long thumbnailMediaId = medias.getFirst().getId(); // 첫 이미지 = 썸네일

        // 4) Feed 생성
        Feed feed = Feed.builder()
                .authorId(userId)
                .content(request.content())
                .tagsNorm(tagsNormArr)
                .visibility(request.visibility() == null ? FeedVisibility.PUBLIC : request.visibility())
                .thumbnailMediaId(thumbnailMediaId)
                .status(FeedStatus.PUBLISHED)
                .build();

        feedRepository.save(feed);

        // 5) FeedMedia 연결 저장(sortOrder 유지)
        List<FeedMedia> links = new ArrayList<>(medias.size());
        for (int i = 0; i < medias.size(); i++) {
            links.add(FeedMedia.create(feed, medias.get(i).getId(), i));
        }
        feedMediaRepository.saveAll(links);

        // 6) 태그 엔티티/관계 저장(기존 유지)
        saveTagRelations(feed.getId(), request.tags());

        // 7) 응답 매핑에 필요한 연관(author/thumbnailMedia) 프록시 부착
        em.flush();
        em.refresh(feed);

        return feedMapper.toResponse(feed, mediaUrlResolver);
    }

    /**
     * 피드는 생성 후 수정 불가 정책.
     * (Controller 유지로 인해 메서드는 남기되, 항상 거부)
     */
    public void changeVisibility(Long userId, Long feedId, ChangeVisibilityRequestDto request) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (!feed.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_UPDATE_OTHER);
        }

        throw new BusinessException(ErrorCode.INVALID_UPDATE_OTHER);
    }

    public void deleteFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (!feed.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_DELETE_OTHER);
        }

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
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Feed> feeds = feedRepository.findFeedsForHome(cursor, pageable);

        List<Feed> filtered = feeds.stream()
                .filter(feed -> !userBlockRepository.existsAnyBlock(viewerId, feed.getAuthorId()))
                .toList();

        boolean hasNext = filtered.size() > limit;
        if (hasNext) {
            filtered = filtered.subList(0, limit);
        }

        Long nextCursor = filtered.isEmpty() ? null : filtered.getLast().getId();

        return new FeedPageDto(
                filtered.stream()
                        .map(feed -> feedMapper.toResponse(feed, mediaUrlResolver))
                        .toList(),
                nextCursor,
                hasNext
        );
    }

    /**
     * FullView:
     * - MediaType으로 이미지/비디오 분기
     * - 첫 조회일 때만 크레딧 차감 + 히스토리 저장
     *
     * NOTE: Controller에서 호출 시 파라미터 순서가 (feedId, userId)로 뒤집혀 있던 부분은
     * 반드시 feedService.getFeedFullView(principal.userId(), feedId)로 고쳐야 합니다.
     */
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
                    .build());
        }

        boolean liked = feedLikeRepository.existsByFeedIdAndUserId(feedId, userId);
        boolean bookmarked = feedBookmarkRepository.existsByFeedIdAndUserId(feedId, userId);

        String videoHlsUrl = null;
        String videoSourceUrl = null;
        List<String> imageUrls = new ArrayList<>();

        // Media까지 확실히 fetch (N+1 방지 목적)
        List<FeedMedia> medias = feedMediaRepository.findWithMediaByFeedId(feedId);

        for (FeedMedia fm : medias) {
            Media m = fm.getMedia();
            if (m == null || m.getMediaType() == null) continue;

            if (m.getMediaType() == MediaType.FEED_VIDEO) {
                String prefix = m.getVideoPrefix();
                if (prefix != null && !prefix.isBlank()) {
                    videoHlsUrl = mediaUrlResolver.hlsManifestUrl(prefix);
                    videoSourceUrl = mediaUrlResolver.videoSourceUrl(prefix, "mp4");
                }
            } else if (m.getMediaType() == MediaType.FEED_IMAGE) {
                String key = m.getObjectKey();
                if (key != null && !key.isBlank()) {
                    imageUrls.add(mediaUrlResolver.toPublicUrl(key));
                }
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
                .commentCount(feed.getCommentCount())
                .viewCount(feed.getViewCount())
                .liked(liked)
                .bookmarked(bookmarked)
                .createdAt(feed.getCreatedAt())
                .build();
    }

    public FeedReportResponseDto reportFeed(Long feedId, Long reporterId, FeedReportRequestDto request) {
        feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        boolean alreadyReported = feedReportRepository.existsByReporterIdAndFeedId(reporterId, feedId);
        if (alreadyReported) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED_FEED);
        }

        FeedReport report = FeedReport.builder()
                .feedId(feedId)
                .reporterId(reporterId)
                .reason(request.reason())
                .description(request.description())
                .createdAt(Instant.now())
                .build();

        feedReportRepository.save(report);

        return new FeedReportResponseDto(
                feedId,
                reporterId,
                "신고가 정상적으로 접수되었습니다."
        );
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
        if (!feedLikeRepository.existsByFeedIdAndUserId(feedId, userId)) {
            throw new BusinessException(ErrorCode.INVALID_LIKE_STATE);
        }

        feedLikeRepository.deleteByUserIdAndFeedId(userId, feedId);

        int updated = feedRepository.decrementLikeCount(feedId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INVALID_LIKE_STATE);
        }
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
        if (!feedBookmarkRepository.existsByFeedIdAndUserId(feedId, userId)) {
            throw new BusinessException(ErrorCode.INVALID_BOOKMARK_STATE);
        }

        feedBookmarkRepository.deleteByUserIdAndFeedId(userId, feedId);

        int updated = feedRepository.decrementBookmarkCount(feedId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INVALID_BOOKMARK_STATE);
        }
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
        if (feedIds == null || feedIds.isEmpty()) {
            return List.of();
        }

        List<Feed> feeds = feedRepository.findByIdIn(feedIds);

        return feeds.stream()
                .map(feed -> feedMapper.toResponse(feed, mediaUrlResolver))
                .toList();
    }

    // -----------------------
    // helpers
    // -----------------------

    private void saveTagRelations(Long feedId, List<String> tags) {
        if (tags == null) return;

        for (String raw : tags) {
            if (raw == null) continue;
            String tagName = raw.toLowerCase().trim();
            if (tagName.isBlank()) continue;

            FeedTag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(
                            FeedTag.builder().name(tagName).build()
                    ));

            feedTagRelationRepository.save(
                    FeedTagRelation.builder()
                            .feedId(feedId)
                            .tagId(tag.getId())
                            .build()
            );
        }
    }

    private String[] normalizeTags(List<String> tags) {
        if (tags == null) return new String[0];

        List<String> norm = new ArrayList<>();
        for (String t : tags) {
            if (t == null) continue;
            String v = t.toLowerCase().trim();
            if (!v.isBlank()) norm.add(v);
        }
        return norm.toArray(String[]::new);
    }

    private String mapImageExtFromMime(String mime) {
        if (mime == null) return null;
        return switch (mime.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/avif" -> "avif";
            default -> null;
        };
    }

    private boolean hasAllowedImageExt(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".avif");
    }

    private String buildFeedImageKey(Long userId, String ext) {
        long now = Instant.now(clock).toEpochMilli();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "users/%d/feeds/images/%d_%s.%s".formatted(userId, now, uuid, ext);
    }

    private String createPresignedPutUrl(String objectKey, String mimeType) {
        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(mediaBucket)
                .key(objectKey)
                .contentType(mimeType)
                .build();

        PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                .putObjectRequest(por)
                .signatureDuration(FEED_IMAGE_PRESIGN_TTL)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presign);
        return presigned.url().toString();
    }
}
