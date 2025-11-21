package org.example.rippleback.features.comment.api;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.comment.api.dto.*;
import org.example.rippleback.features.comment.app.CommentService;
import org.example.rippleback.features.comment.domain.Comment;
import org.example.rippleback.features.comment.domain.CommentReport;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/feeds/{feedId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto createComment(@PathVariable Long feedId,
                                            @Validated @RequestBody CommentCreateRequestDto request,
                                            @AuthenticationPrincipal Long meId
    ) {
        Comment comment = commentService.create(
                meId,
                feedId,
                request.parentId(),
                request.content()
        );
        return CommentResponseDto.from(comment);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId,
                              @AuthenticationPrincipal Long meId
    ) {
        commentService.delete(meId, commentId);
    }

    @PostMapping("/comments/{commentId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likeComment(@PathVariable Long commentId,
                            @AuthenticationPrincipal Long meId
    ) {
        commentService.like(meId, commentId);
    }

    @DeleteMapping("/comments/{commentId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikeComment(@PathVariable Long commentId,
                              @AuthenticationPrincipal Long meId
    ) {
        commentService.unlike(meId, commentId);
    }

    @PostMapping("/comments/{commentId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentReportResponseDto reportComment(@PathVariable Long commentId,
                                                  @Validated @RequestBody CommentReportRequestDto request,
                                                  @AuthenticationPrincipal Long meId
    ) {
        CommentReport report = commentService.report(
                meId,
                commentId,
                request.category(),
                request.reason()
        );
        return CommentReportResponseDto.from(report);
    }

    @GetMapping("/feeds/{feedId}/comments")
    public CommentPageResponseDto getRootComments(
            @PathVariable Long feedId,
            @Validated CommentPageRequestDto request,
            @AuthenticationPrincipal Long meId
    ) {
        return commentService.getRootComments(
                meId,
                feedId,
                request.cursorId(),
                request.size()
        );
    }

    @GetMapping("/feeds/{feedId}/comments/{commentId}/replies")
    public CommentPageResponseDto getReplies(
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @Validated CommentPageRequestDto request,
            @AuthenticationPrincipal Long meId
    ) {
        return commentService.getReplies(
                meId,
                feedId,
                commentId,
                request.cursorId(),
                request.size()
        );
    }
}
