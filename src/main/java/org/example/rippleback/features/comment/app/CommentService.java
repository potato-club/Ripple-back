package org.example.rippleback.features.comment.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import org.example.rippleback.features.comment.api.dto.CommentPageResponseDto;
import org.example.rippleback.features.comment.api.dto.CommentResponseDto;
import org.example.rippleback.features.comment.domain.*;
import org.example.rippleback.features.comment.infra.CommentLikeRepository;
import org.example.rippleback.features.comment.infra.CommentReportRepository;
import org.example.rippleback.features.comment.infra.CommentRepository;
import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedStatus;
import org.example.rippleback.features.feed.domain.FeedVisibility;
import org.example.rippleback.features.feed.infra.FeedRepository;
import org.example.rippleback.features.user.api.dto.UserSummaryDto;
import org.example.rippleback.features.user.app.UserMapper;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.features.user.infra.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final UserRepository userRepo;
    private final CommentRepository commentRepo;
    private final CommentLikeRepository commentLikeRepo;
    private final CommentReportRepository commentReportRepo;
    private final FeedRepository feedRepo;
    private final UserMapper userMapper;

    @Transactional
    public CommentResponseDto  create(Long authorId, Long feedId, Long parentId, String content) {
        if (content == null || content.isBlank() || content.length() > 3000) {
            throw new BusinessException(ErrorCode.COMMENT_CONTENT_INVALID);
        }

        Feed post = feedRepo.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        assertFeedVisibleToMeOr404(post, authorId);

        Long rootId = null;
        Long replyToUserId = null;
        Long replyToCommentId = null;

        if (parentId != null) {
            Comment parent = commentRepo.findById(parentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

            if (parent.isDeleted()) {
                throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
            }

            if (!parent.getFeedId().equals(feedId)) {
                throw new BusinessException(ErrorCode.INVALID_COMMENT_THREAD);
            }

            rootId = (parent.getRootCommentId() == null)
                    ? parent.getId()
                    : parent.getRootCommentId();

            replyToUserId = parent.getAuthorId();
            replyToCommentId = parent.getId();
        }

        Comment saved = commentRepo.save(Comment.builder()
                .feedId(feedId)
                .authorId(authorId)
                .rootCommentId(rootId)
                .replyToUserId(replyToUserId)
                .content(content)
                .status(CommentStatus.PUBLISHED)
                .createdAt(Instant.now())
                .build());

        feedRepo.incrementCommentCount(feedId);

        User author = userRepo.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        UserSummaryDto authorSummary = userMapper.toSummary(author);

        return CommentResponseDto.from(saved, authorSummary);
    }


    @Transactional
    public void delete(Long authorId, Long commentId) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (c.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        if (!c.getAuthorId().equals(authorId)) {
            throw new BusinessException(ErrorCode.COMMENT_DELETE_NOT_ALLOWED);
        }

        boolean wasVisible = c.isVisible();
        boolean isRoot = c.isRoot();

        if (isRoot) {
            boolean hasVisibleChild =
                    commentRepo.existsByRootCommentIdAndVisibility(
                            c.getId(),
                            CommentVisibility.VISIBLE
                    );

            if (hasVisibleChild) {
                c.markDeletedButVisibleWithMask();
            } else {
                c.markDeletedHidden();
            }
        } else {
            c.markDeletedHidden();
        }

        commentLikeRepo.deleteAllByCommentId(commentId);

        if (wasVisible && !c.isVisible()) {
            feedRepo.decrementCommentCount(c.getFeedId());
        }

        commentRepo.save(c);
    }

    @Transactional
    public void like(Long authorId, Long commentId) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (c.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        int inserted = commentLikeRepo.insertIgnore(authorId, commentId);
        if (inserted > 0) {
            commentRepo.incLikeCount(commentId);
        }
    }

    @Transactional
    public void unlike(Long authorId, Long commentId) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (c.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        int deleted = commentLikeRepo.deleteOne(authorId, commentId);
        if (deleted > 0) {
            commentRepo.decLikeCount(commentId);
        }
    }


    @Transactional
    public CommentReport report(Long reporterId,
                                Long commentId,
                                CommentReportCategory category,
                                String reason) {

        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (c.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        var existingOpt = commentReportRepo.findByReporterIdAndCommentId(reporterId, commentId);

        if (existingOpt.isPresent()) {
            CommentReport existing = existingOpt.get();

            String customMessage = switch (existing.getStatus()) {
                case REVIEWING -> "이미 신고가 접수되어 검토 중입니다.";
                case RESOLVED -> "이미 처리된 신고입니다.";
                case REJECTED -> existing.getNotes();
            };

            throw new BusinessException(ErrorCode.ALREADY_REPORTED_COMMENT, customMessage);
        }

        CommentReport saved = commentReportRepo.save(
                CommentReport.builder()
                        .reporterId(reporterId)
                        .commentId(commentId)
                        .category(category)
                        .reason(reason)
                        .status(CommentReportStatus.REVIEWING)
                        .createdAt(Instant.now())
                        .build()
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public CommentPageResponseDto getRootComments(
            Long viewerId,
            Long feedId,
            Long cursorId,
            int size
    ) {
        Feed feed = feedRepo.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        assertFeedVisibleToMeOr404(feed, viewerId);

        if (size <= 0) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(0, size);

        List<Comment> comments = commentRepo.findRootComments(feedId, cursorId, pageable);

        if (comments.isEmpty()) {
            return new CommentPageResponseDto(List.of(), null, false);
        }

        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());

        List<User> users = authorIds.isEmpty()
                ? List.of()
                : userRepo.findByIdInWithProfile(authorIds);

        Map<Long, UserSummaryDto> authorMap = users.stream()
                .map(userMapper::toSummary)
                .collect(Collectors.toMap(UserSummaryDto::id, it -> it));

        List<CommentResponseDto> dtos = comments.stream()
                .map(c -> {
                    UserSummaryDto author = authorMap.get(c.getAuthorId());
                    if (author == null) {
                        author = new UserSummaryDto(c.getAuthorId(), "Ripple User", null);
                    }
                    return CommentResponseDto.from(c, author);
                })
                .toList();

        Comment last = comments.get(comments.size() - 1);
        Long nextCursor = comments.size() == size ? last.getId() : null;
        boolean hasNext = nextCursor != null;

        return new CommentPageResponseDto(dtos, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public CommentPageResponseDto getReplies(
            Long viewerId,
            Long feedId,
            Long rootCommentId,
            Long cursorId,
            int size
    ) {
        Feed feed = feedRepo.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        assertFeedVisibleToMeOr404(feed, viewerId);

        Comment root = commentRepo.findById(rootCommentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (!root.getFeedId().equals(feedId)) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_THREAD);
        }

        if (size <= 0) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(0, size);

        List<Comment> replies = commentRepo.findReplies(feedId, rootCommentId, cursorId, pageable);

        if (replies.isEmpty()) {
            return new CommentPageResponseDto(List.of(), null, false);
        }

        Set<Long> authorIds = replies.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());

        List<User> users = authorIds.isEmpty()
                ? List.of()
                : userRepo.findByIdInWithProfile(authorIds);

        Map<Long, UserSummaryDto> authorMap = users.stream()
                .map(userMapper::toSummary)
                .collect(Collectors.toMap(UserSummaryDto::id, it -> it));

        List<CommentResponseDto> dtos = replies.stream()
                .map(c -> {
                    UserSummaryDto author = authorMap.get(c.getAuthorId());
                    if (author == null) {
                        author = new UserSummaryDto(c.getAuthorId(), "Ripple User", null);
                    }
                    return CommentResponseDto.from(c, author);
                })
                .toList();

        Comment last = replies.get(replies.size() - 1);
        Long nextCursor = replies.size() == size ? last.getId() : null;
        boolean hasNext = nextCursor != null;

        return new CommentPageResponseDto(dtos, nextCursor, hasNext);
    }


    private void assertFeedVisibleToMeOr404(Feed feed, Long userId) {
        if (feed.getStatus() != FeedStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
        }

        if (feed.getVisibility() == FeedVisibility.FOLLOWERS) {

        }
        // TODO : 유저의 차단 관계 같은 것에 따라서 피드가 유저에게 공개되었는지 같은 것들 설정
    }
}
