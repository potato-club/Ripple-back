package org.example.rippleback.features.comment.infra;

import org.example.rippleback.features.comment.domain.Comment;
import org.example.rippleback.features.comment.domain.CommentVisibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Modifying
    @Query(value = """
                UPDATE comment
                SET like_count = like_count + 1
                WHERE id = ?1
            """, nativeQuery = true)
    int incLikeCount(Long commentId);

    @Modifying
    @Query(value = """
                UPDATE comment
                SET like_count = GREATEST(like_count - 1, 0)
                WHERE id = ?1
            """, nativeQuery = true)
    int decLikeCount(Long commentId);

    @Query("""
                select c from Comment c
                where c.feedId = :feedId
                  and c.rootCommentId is null
                  and c.visibility = org.example.rippleback.features.comment.domain.CommentVisibility.VISIBLE
                  and (:cursorId is null or c.id < :cursorId)
                order by c.id desc
            """)
    List<Comment> findRootComments(
            @Param("feedId") Long feedId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
    select c from Comment c
    where c.feedId = :feedId
      and c.rootCommentId is null
      and c.visibility = org.example.rippleback.features.comment.domain.CommentVisibility.VISIBLE
    order by c.likeCount desc, c.id desc
    """)
    List<Comment> findRootCommentsOrderByLikeCountDesc(
            @Param("feedId") Long feedId,
            Pageable pageable
    );



    @Query("""
                select c from Comment c
                where c.feedId = :feedId
                  and c.rootCommentId = :rootId
                  and c.visibility = org.example.rippleback.features.comment.domain.CommentVisibility.VISIBLE
                  and (:cursorId is null or c.id < :cursorId)
                order by c.id desc
            """)
    List<Comment> findReplies(
            @Param("feedId") Long feedId,
            @Param("rootId") Long rootId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
    select c from Comment c
    where c.feedId = :feedId
      and c.rootCommentId = :rootId
      and c.visibility = org.example.rippleback.features.comment.domain.CommentVisibility.VISIBLE
    order by c.likeCount desc, c.id desc
    """)
    List<Comment> findRepliesOrderByLikeCountDesc(
            @Param("feedId") Long feedId,
            @Param("rootId") Long rootId,
            Pageable pageable
    );

    boolean existsByRootCommentIdAndVisibility(Long rootCommentId, CommentVisibility visibility);

    // 다른 사용자가 유저 프로필에 접근했을 때 그 사용자에게 보여줄 수 있는 댓글 최신순으로 찾기
    @Query("""
    select c
    from Comment c
    join c.feed f
    where c.authorId = :authorId
      and c.status = org.example.rippleback.features.comment.domain.CommentStatus.PUBLISHED
      and c.visibility = org.example.rippleback.features.comment.domain.CommentVisibility.VISIBLE
      and f.status = org.example.rippleback.features.feed.domain.FeedStatus.PUBLISHED
      and f.visibility = org.example.rippleback.features.feed.domain.FeedVisibility.PUBLIC
    order by c.id desc
""")
    List<Comment> findLatestVisiblePublicByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

}
