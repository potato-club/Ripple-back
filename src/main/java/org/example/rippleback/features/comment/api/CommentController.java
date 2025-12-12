package org.example.rippleback.features.comment.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.comment.api.dto.*;
import org.example.rippleback.features.comment.app.CommentService;
import org.example.rippleback.features.comment.domain.CommentSortType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "comments", description = "댓글/대댓글/댓글 좋아요/댓글 신고 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "댓글 작성",
            description = """
                    피드에 댓글을 작성합니다.
                    - parentId가 null이면 루트 댓글
                    - parentId가 있으면 대댓글(답글)이며, parent 댓글의 feedId와 요청 feedId가 다르면 실패합니다.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 형식/검증 오류 또는 비즈니스 규칙 위반 (예: INVALID_COMMENT_THREAD, COMMENT_CONTENT_INVALID)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "피드/부모댓글을 찾을 수 없음 (예: FEED_NOT_FOUND, COMMENT_NOT_FOUND)")
    })
    @PostMapping("/feeds/{feedId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto createComment(
            @Parameter(description = "피드 ID", required = true, example = "123")
            @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long meId
    ) {
        return commentService.create(
                meId,
                feedId,
                request.parentId(),
                request.content()
        );
    }

    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 삭제합니다. 존재하지 않거나 이미 삭제된 댓글이면 404로 처리될 수 있습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음 (예: COMMENT_NOT_FOUND)")
    })
    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @Parameter(description = "댓글 ID", required = true, example = "987")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long meId
    ) {
        commentService.delete(meId, commentId);
    }

    @Operation(
            summary = "댓글 좋아요",
            description = "댓글에 좋아요를 추가합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음 (예: COMMENT_NOT_FOUND)")
    })
    @PostMapping("/comments/{commentId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likeComment(
            @Parameter(description = "댓글 ID", required = true, example = "987")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long meId
    ) {
        commentService.like(meId, commentId);
    }

    @Operation(
            summary = "댓글 좋아요 취소",
            description = "댓글 좋아요를 취소합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음 (예: COMMENT_NOT_FOUND)")
    })
    @DeleteMapping("/comments/{commentId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikeComment(
            @Parameter(description = "댓글 ID", required = true, example = "987")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long meId
    ) {
        commentService.unlike(meId, commentId);
    }

    @Operation(
            summary = "댓글 신고",
            description = """
                    댓글을 신고합니다.
                    - 동일 사용자가 동일 댓글을 중복 신고하면 실패합니다.
                    - 기존 신고 상태(REVIEWING/RESOLVED/REJECTED)에 따라 메시지가 달라질 수 있습니다.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "신고 접수 성공",
                    content = @Content(schema = @Schema(implementation = CommentReportResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 형식/검증 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음 (예: COMMENT_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "이미 신고한 댓글 (예: ALREADY_REPORTED_COMMENT)")
    })
    @PostMapping("/comments/{commentId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentReportResponseDto reportComment(
            @Parameter(description = "댓글 ID", required = true, example = "987")
            @PathVariable Long commentId,
            @Valid @RequestBody CommentReportRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long meId
    ) {
        return CommentReportResponseDto.from(
                commentService.report(
                        meId,
                        commentId,
                        request.category(),
                        request.reason()
                )
        );
    }

    @Operation(
            summary = "피드 루트 댓글 조회",
            description = """
                    피드의 루트 댓글을 커서 기반으로 조회합니다.
                    - sort=LATEST: cursorId 기반 페이징(nextCursor/hasNext 제공)
                    - sort=MOST_LIKED: 좋아요순(커서 페이징 비활성: nextCursor=null, hasNext=false)
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CommentPageResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 형식/검증 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음 (예: FEED_NOT_FOUND)")
    })
    @GetMapping("/feeds/{feedId}/comments")
    public CommentPageResponseDto getRootComments(
            @Parameter(description = "피드 ID", required = true, example = "123")
            @PathVariable Long feedId,
            @Valid CommentPageRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long meId
    ) {
        int size = (request.size() == null || request.size() <= 0) ? 10 : request.size();
        var sort = request.sort() == null ? CommentSortType.LATEST : request.sort();

        return commentService.getRootComments(
                meId,
                feedId,
                request.cursorId(),
                size,
                sort
        );
    }

    @Operation(
            summary = "대댓글 조회",
            description = """
                    특정 루트 댓글(commentId)의 대댓글 목록을 커서 기반으로 조회합니다.
                    - 요청 feedId와 루트 댓글의 feedId가 다르면 실패합니다(INVALID_COMMENT_THREAD).
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CommentPageResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 형식/검증 오류 또는 비즈니스 규칙 위반 (예: INVALID_COMMENT_THREAD)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "피드/댓글을 찾을 수 없음 (예: FEED_NOT_FOUND, COMMENT_NOT_FOUND)")
    })
    @GetMapping("/feeds/{feedId}/comments/{commentId}/replies")
    public CommentPageResponseDto getReplies(
            @Parameter(description = "피드 ID", required = true, example = "123")
            @PathVariable Long feedId,
            @Parameter(description = "루트 댓글 ID", required = true, example = "987")
            @PathVariable Long commentId,
            @Valid CommentPageRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long meId
    ) {
        int size = (request.size() == null || request.size() <= 0) ? 10 : request.size();
        var sort = request.sort() == null ? CommentSortType.LATEST : request.sort();

        return commentService.getReplies(
                meId,
                feedId,
                commentId,
                request.cursorId(),
                size,
                sort
        );
    }
}
