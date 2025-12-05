package org.example.rippleback.features.feed.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "feed_tag_relations",
        indexes = {
                @Index(name = "ix_feed_tag_feed", columnList = "feed_id"),
                @Index(name = "ix_feed_tag_tag", columnList = "tag_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_feed_tag_unique", columnNames = {"feed_id", "tag_id"})
        }
)
public class FeedTagRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feed_id", nullable = false)
    private Long feedId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
