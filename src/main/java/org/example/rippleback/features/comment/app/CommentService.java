package org.example.rippleback.features.comment.app;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import org.example.rippleback.features.comment.domain.*;
import org.example.rippleback.features.comment.infra.CommentLikeRepository;
import org.example.rippleback.features.comment.infra.CommentReportRepository;
import org.example.rippleback.features.comment.infra.CommentRepository;
import org.example.rippleback.features.post.domain.Post;
import org.example.rippleback.features.post.domain.PostStatus;
import org.example.rippleback.features.post.infra.PostRepository;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepo;
    private final CommentLikeRepository commentLikeRepo;
    private final CommentReportRepository commentReportRepo;
    private final PostRepository postRepo;


    @Transactional
    public Comment create(Long me, Long postId, Long parentId, String content) {
        if (content == null || content.isBlank() || content.length() > 3000) {
            throw new BusinessException(ErrorCode.COMMENT_CONTENT_INVALID);
        }

        // 포스트 가시성(미발행/비공개/차단 등 404 마스킹)
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        assertPostVisibleToMeOr404(post, me);

        // 스레드 일관성
        Long rootId = null;
        if (parentId != null) {
            Comment parent = commentRepo.findById(parentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

            if (!parent.getPostId().equals(postId)) {
                throw new BusinessException(ErrorCode.INVALID_COMMENT_THREAD);
            }
            rootId = parent.getRootCommentId() != null ? parent.getRootCommentId() : parent.getId();
        }

        Comment saved = commentRepo.save(Comment.builder()
                .postId(postId)
                .authorId(me)
                .parentCommentId(parentId)
                .rootCommentId(rootId)
                .content(content)
                .status(CommentStatus.PUBLISHED)
                .createdAt(Instant.now())
                .build());

        postRepo.incrementComment(postId); // 카운터 +1 (같은 트랜잭션)
        return saved;
    }

    @Transactional
    public void delete(Long me, Long commentId) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        // 본인만 삭제(관리자 룰이 있으면 추가)
        if (!c.getAuthorId().equals(me)) {
            throw new BusinessException(ErrorCode.COMMENT_DELETE_NOT_ALLOWED);
        }

        if (!c.isDeleted()) {                // 멱등
            c.softDelete();                  // status=DELETED, deleted_at 설정
            commentRepo.save(c);
            postRepo.decrementComment(c.getPostId()); // 카운터 -1
        }
    }

    /* ========= Like (멱등) ========= */

    @Transactional
    public void like(Long me, Long commentId) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (c.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND); // 마스킹
        }

        int inserted = commentLikeRepo.insertIgnore(me, commentId);
        if (inserted > 0) {
            commentRepo.incLikeCount(commentId);
        }
        // 항상 204로 응답(컨트롤러에서)
    }

    @Transactional
    public void unlike(Long me, Long commentId) {
        int deleted = commentLikeRepo.deleteOne(me, commentId);
        if (deleted > 0) {
            commentRepo.decLikeCount(commentId);
        }
        // 항상 204
    }

    /* ========= Report (멱등) ========= */

    @Transactional
    public Long report(Long me, Long commentId, String reason) {
        // 댓글 존재 여부만 확인(가시성 실패는 404 마스킹)
        commentRepo.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        var existing = commentReportRepo.findByReporterIdAndCommentIdAndStatusIn(
                me, commentId, List.of(ReportStatus.OPEN, ReportStatus.REVIEWING)
        ).orElse(null);

        if (existing != null) {
            // 멱등: 예외 던지지 않음. 컨트롤러에서 204 또는 200 처리.
            return existing.getId();
        }

        var saved = commentReportRepo.save(CommentReport.builder()
                .reporterId(me)
                .commentId(commentId)
                .reason(reason)
                .status(ReportStatus.OPEN)
                .createdAt(Instant.now())
                .build());

        return saved.getId(); // 컨트롤러에서 201
    }

    /* ========= 내부 검증 ========= */

    private void assertPostVisibleToMeOr404(Post post, Long me) {
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
    }
}
