package org.example.rippleback.features.comment.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import org.example.rippleback.features.comment.domain.*;
import org.example.rippleback.features.comment.infra.CommentLikeRepository;
import org.example.rippleback.features.comment.infra.CommentReportRepository;
import org.example.rippleback.features.comment.infra.CommentRepository;
import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedStatus;
import org.example.rippleback.features.feed.infra.FeedRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepo;
    private final CommentLikeRepository commentLikeRepo;
    private final CommentReportRepository commentReportRepo;
    private final FeedRepository feedRepo;

    @Transactional
    public Comment create(Long authorId, Long feedId, Long parentId, String content) {
        if (content == null || content.isBlank() || content.length() > 300) {
            throw new BusinessException(ErrorCode.COMMENT_CONTENT_INVALID);
        }

        Feed feed = feedRepo.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        assertFeedVisibleToMeOr404(feed, authorId);

        Long rootId = null;
        if (parentId != null) {
            Comment parent = commentRepo.findById(parentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

            if (parent.isDeleted()) {
                throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
            }

            if (!parent.getFeedId().equals(feedId)) {
                throw new BusinessException(ErrorCode.INVALID_COMMENT_THREAD);
            }

            rootId = (parent.getRootCommentId() != null)
                    ? parent.getRootCommentId()
                    : parent.getId();
        }

        Comment saved = commentRepo.save(Comment.builder()
                .feedId(feedId)
                .authorId(authorId)
                .parentCommentId(parentId)
                .rootCommentId(rootId)
                .content(content)
                .status(CommentStatus.PUBLISHED)
                .createdAt(Instant.now())
                .build());

        feedRepo.incrementCommentCount(feedId);

        return saved;
    }

    @Transactional
    public void delete(Long authorId, Long commentId) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!c.getAuthorId().equals(authorId)) {
            throw new BusinessException(ErrorCode.COMMENT_DELETE_NOT_ALLOWED);
        }

        if (!c.isDeleted()) {
            c.softDelete();
            commentRepo.save(c);
            feedRepo.decrementCommentCount(c.getFeedId());
        }
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
        int deleted = commentLikeRepo.deleteOne(authorId, commentId);
        if (deleted > 0) {
            commentRepo.decLikeCount(commentId);
        }
    }

    @Transactional
    public CommentReport  report(Long reporterId,
                       Long commentId,
                       ReportCategory category,
                       String reason) {

        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (c.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        boolean alreadyReported =
                commentReportRepo.existsByReporterIdAndCommentId(reporterId, commentId);

        if (alreadyReported) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED_COMMENT);
        }

        var existingOpt = commentReportRepo.findByReporterIdAndCommentId(reporterId, commentId);

        if (existingOpt.isPresent()) {
            CommentReport existing = existingOpt.get();

            String customMessage = switch (existing.getStatus()) {
                case REVIEWING -> "이미 신고가 접수되어 검토 중입니다.";
                case RESOLVED  -> "이미 처리된 신고입니다.";
                case REJECTED  -> existing.getNotes();
            };

            throw new BusinessException(ErrorCode.ALREADY_REPORTED_COMMENT, customMessage);
        }

        CommentReport saved = commentReportRepo.save(
                CommentReport.builder()
                        .reporterId(reporterId)
                        .commentId(commentId)
                        .category(category)
                        .reason(reason)
                        .status(ReportStatus.REVIEWING)
                        .createdAt(Instant.now())
                        .build()
        );

        return saved;
    }

    private void assertFeedVisibleToMeOr404(Feed feed, Long me) { // 가시성 관련
        if (feed.getStatus() != FeedStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
        }
    }
}
