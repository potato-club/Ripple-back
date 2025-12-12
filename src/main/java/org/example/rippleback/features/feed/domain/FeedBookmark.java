package org.example.rippleback.features.feed.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"feed_id", "user_id"}))
public class FeedBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id",  nullable = false, updatable = false)
    private Feed feed;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static FeedBookmark create(Feed feed, Long userId) {
        return new FeedBookmark(
                null,
                userId,
                feed,
                Instant.now()
        );
    }
}
