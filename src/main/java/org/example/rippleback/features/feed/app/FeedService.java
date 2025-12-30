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

    private static final long MAX_FEED_VIDEO_BYTES = 200 * 1024 * 1024L; // 200MB
    private static final Duration FEED_VIDEO_PRESIGN_TTL = Duration.ofMinutes(20);
    private static final int MIN_FEED_VIDEO_DURATION_SEC = 3;
    private static final int MAX_FEED_VIDEO_DURATION_SEC = 180;

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

    @Value("${app.media.bucket}")
    private String mediaBucket;

    /**
     * 피드 이미지 업로드용 presigned PUT URL 발급 (최대 15장)
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
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }

            String ext = mapImageExtFromMime(f.mimeType());
            if (ext == null) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }

            String objectKey = buildFeedImageKey(userId, ext);
            String uploadUrl = createPresignedPutUrl(objectKey, f.mimeType(), FEED_IMAGE_PRESIGN_TTL);

            items.add(new FeedImagePresignResponseDto.Item(
                    uploadUrl,
                    objectKey,
                    MAX_FEED_IMAGE_BYTES
            ));
        }

        return new FeedImagePresignResponseDto(items);
    }

    /**
     * 피드 비디오 업로드용 presigned PUT URL 발급 (비디오 1개 + 썸네일 1개)
     * - 비디오는 prefix 아래에 보관
     * ex) users/{userId}/feeds/videos/{token}/source.mp4
     * users/{userId}/feeds/videos/{token}/thumb.jpg
     */
    @Transactional(readOnly = true)
    public FeedVideoPresignResponseDto prepareFeedVideoUploads(Long userId, FeedVideoPresignRequestDto request) {
        if (request == null || request.video() == null || request.thumbnail() == null) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        FeedVideoPresignRequestDto.VideoSpec v = request.video();
        FeedVideoPresignRequestDto.ThumbnailSpec t = request.thumbnail();

        long vSize = (v.sizeBytes() == null) ? -1L : v.sizeBytes();
        if (vSize <= 0 || vSize > MAX_FEED_VIDEO_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        Integer dur = v.durationSec();
        if (dur == null || dur < MIN_FEED_VIDEO_DURATION_SEC || dur > MAX_FEED_VIDEO_DURATION_SEC) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        String vExt = mapVideoExtFromMime(v.mimeType());
        if (vExt == null) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        long tSize = (t.sizeBytes() == null) ? -1L : t.sizeBytes();
        if (tSize <= 0 || tSize > MAX_FEED_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        String tExt = mapImageExtFromMime(t.mimeType());
        if (tExt == null) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        String prefix = buildFeedVideoPrefix(userId);
        String videoObjectKey = prefix + "/source." + vExt;
        String thumbnailObjectKey = prefix + "/thumb." + tExt;

        String videoUploadUrl = createPresignedPutUrl(videoObjectKey, v.mimeType(), FEED_VIDEO_PRESIGN_TTL);
        String thumbnailUploadUrl = createPresignedPutUrl(thumbnailObjectKey, t.mimeType(), FEED_IMAGE_PRESIGN_TTL);

        return new FeedVideoPresignResponseDto(
                prefix,
                videoObjectKey,
                videoUploadUrl,
                thumbnailObjectKey,
                thumbnailUploadUrl,
                MAX_FEED_VIDEO_BYTES,
                MAX_FEED_IMAGE_BYTES,
                MIN_FEED_VIDEO_DURATION_SEC,
                MAX_FEED_VIDEO_DURATION_SEC
        );
    }

    /**
     * 피드 생성
     * - 이미지형: request.imageObjectKeys 사용 (최대 15장)
     * - 비디오형: request.video 사용 (비디오 1개 + 썸네일 1개)
     * <p>
     * 정책:
     * - 피드는 이미지와 비디오를 동시에 가질 수 없음(XOR)
     * - 비디오는 1개만 가능
     * - 썸네일: 이미지형은 첫 이미지, 비디오형은 썸네일 이미지
     * <p>
     * Media.ownerId는 항상 userId로 저장 (Feed와의 연결은 FeedMedia가 담당)
     */
    public FeedResponseDto createFeed(Long userId, FeedRequestDto request) {
        String[] tagsNormArr = normalizeTags(request.tags());

        List<FeedRequestDto.FeedImagePatch> imagesRaw = (request.images() == null)
                ? List.of()
                : request.images();

        FeedRequestDto.FeedVideoPatch  video = request.video();

        boolean hasImages = !imagesRaw.isEmpty();
        boolean hasVideo = video != null && video.videoPrefix() != null && !video.videoPrefix().isBlank();

        // XOR 강제: 둘 다 있거나 둘 다 없으면 거부
        if (hasImages == hasVideo) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        if (hasImages) {
            return createImageFeed(userId, request, tagsNormArr, imagesRaw);
        }
        return createVideoFeed(userId, request, tagsNormArr, video);
    }

    private FeedResponseDto createImageFeed(Long userId, FeedRequestDto request, String[] tagsNormArr, List<FeedRequestDto.FeedImagePatch> imagesRaw) {
        if (imagesRaw.size() > MAX_FEED_IMAGES) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        String expectedPrefix = "users/" + userId + "/feeds/images/";

        // normalize + 중복 방지
        Set<String> seen = new HashSet<>();
        List<FeedRequestDto.FeedImagePatch> images = new ArrayList<>(imagesRaw.size());

        for (FeedRequestDto.FeedImagePatch img : imagesRaw) {
            if (img == null || img.objectKey() == null || img.objectKey().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }

            String key = img.objectKey().trim();

            if (!key.startsWith(expectedPrefix)) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }
            if (!hasAllowedImageExt(key)) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }
            if (!seen.add(key)) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
            }

            // 메타데이터는 있으면 사용, 없으면 null 저장
            String mimeType = (img.mimeType() == null || img.mimeType().isBlank()) ? null : img.mimeType().trim();

            images.add(new FeedRequestDto.FeedImagePatch(
                    key,
                    mimeType,
                    img.width(),
                    img.height(),
                    img.sizeBytes()
            ));
        }

        // 1) Media 생성 (ownerId=userId 고정)
        List<Media> medias = new ArrayList<>(images.size());
        for (FeedRequestDto.FeedImagePatch img : images) {
            medias.add(Media.newFeedImage(
                    userId,
                    img.objectKey(),
                    img.mimeType(),
                    img.width(),
                    img.height(),
                    img.sizeBytes()
            ));
        }
        medias = mediaRepository.saveAll(medias);

        Long thumbnailMediaId = medias.getFirst().getId(); // 첫 이미지 = 썸네일

        // 2) Feed 생성
        Feed feed = Feed.builder()
                .authorId(userId)
                .content(request.content())
                .tagsNorm(tagsNormArr)
                .visibility(request.visibility() == null ? FeedVisibility.PUBLIC : request.visibility())
                .thumbnailMediaId(thumbnailMediaId)
                .status(FeedStatus.PUBLISHED)
                .build();
        feedRepository.save(feed);

        // 3) FeedMedia 연결(sortOrder)
        List<FeedMedia> links = new ArrayList<>(medias.size());
        for (int i = 0; i < medias.size(); i++) {
            links.add(FeedMedia.create(feed, medias.get(i).getId(), i));
        }
        feedMediaRepository.saveAll(links);

        // 4) 태그 관계 저장(기존 유지)
        saveTagRelations(feed.getId(), request.tags());

        // 5) 썸네일 무결성 체크(선택 정책: 첫 이미지 = 썸네일)
        em.flush();

        em.refresh(feed);
        return feedMapper.toResponse(feed, mediaUrlResolver);
    }

    private FeedResponseDto createVideoFeed(
            Long userId,
            FeedRequestDto request,
            String[] tagsNormArr,
            FeedRequestDto.FeedVideoPatch video
    ) {
        if (video == null) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        // videoPrefix는 @S3ObjectKey(allowPrefix=true)로 "형식" 검증된다는 전제
        String prefix = normalizePrefix(video.videoPrefix());

        // 도메인 규칙(유저별/기능별 경로 규칙)은 서비스에서 추가 검증
        String expectedBase = "users/" + userId + "/feeds/videos/";
        if (prefix == null || !prefix.startsWith(expectedBase)) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        // duration
        Integer dur = video.durationSec();
        if (dur == null || dur < MIN_FEED_VIDEO_DURATION_SEC || dur > MAX_FEED_VIDEO_DURATION_SEC) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        // size
        long vSize = (video.sizeBytes() == null) ? -1L : video.sizeBytes();
        if (vSize <= 0 || vSize > MAX_FEED_VIDEO_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        // mime / ext 정책(현재 mp4만 허용)
        String videoMime = (video.mimeType() == null) ? null : video.mimeType().trim();
        String vExt = mapVideoExtFromMime(videoMime);
        if (vExt == null) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        // thumbnail (FeedImagePatch 재사용)
        FeedRequestDto.FeedImagePatch thumb = video.thumbnail();
        if (thumb == null) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        String thumbKey = (thumb.objectKey() == null) ? null : thumb.objectKey().trim();
        if (thumbKey == null || thumbKey.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        // prefix 하위 + 이미지 확장자 + (권장) thumb.* 규칙
        if (!thumbKey.startsWith(prefix + "/")) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }
        if (!hasAllowedImageExt(thumbKey)) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }
        if (!thumbKey.startsWith(prefix + "/thumb.")) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        String thumbMime = (thumb.mimeType() == null) ? null : thumb.mimeType().trim();

        // 1) Media 생성/저장
        Media videoMedia = Media.newFeedVideo(
                userId,
                prefix,
                videoMime,
                video.width(),
                video.height(),
                dur,
                vSize
        );

        Media thumbnailMedia = Media.newFeedImage(
                userId,
                thumbKey,
                thumbMime,
                thumb.width(),
                thumb.height(),
                thumb.sizeBytes()
        );

        videoMedia = mediaRepository.save(videoMedia);
        thumbnailMedia = mediaRepository.save(thumbnailMedia);

        // 2) Feed 생성 (thumbnailMediaId는 썸네일)
        Feed feed = Feed.builder()
                .authorId(userId)
                .content(request.content())
                .tagsNorm(tagsNormArr)
                .visibility(request.visibility() == null ? FeedVisibility.PUBLIC : request.visibility())
                .thumbnailMediaId(thumbnailMedia.getId())
                .status(FeedStatus.PUBLISHED)
                .build();
        feedRepository.save(feed);

        // 3) FeedMedia 연결: 비디오만(1개)
        feedMediaRepository.save(FeedMedia.create(feed, videoMedia.getId(), 0));

        // 4) 태그 관계 저장
        saveTagRelations(feed.getId(), request.tags());

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
     * <p>
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

    private String mapVideoExtFromMime(String mime) {
        if (mime == null) return null;
        return switch (mime.toLowerCase(Locale.ROOT)) {
            case "video/mp4" -> "mp4";
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

    private String buildFeedVideoPrefix(Long userId) {
        long now = Instant.now(clock).toEpochMilli();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "users/%d/feeds/videos/%d_%s".formatted(userId, now, uuid);
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null) return null;
        String p = prefix.trim();
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private String createPresignedPutUrl(String objectKey, String mimeType, Duration ttl) {
        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(mediaBucket)
                .key(objectKey)
                .contentType(mimeType)
                .build();

        PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                .putObjectRequest(por)
                .signatureDuration(ttl)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presign);
        return presigned.url().toString();
    }
}
