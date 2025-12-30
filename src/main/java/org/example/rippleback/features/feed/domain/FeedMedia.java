package org.example.rippleback.features.feed.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.rippleback.features.media.domain.Media;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Entity
@Table(
        name = "feed_media",
        indexes = {
                @Index(name = "ix_feed_media_feed_sort", columnList = "feed_id, sort_order"),
                @Index(name = "ix_feed_media_media", columnList = "media_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_feed_media_feed_media", columnNames = {"feed_id", "media_id"}),
                @UniqueConstraint(name = "uk_feed_media_feed_sort", columnNames = {"feed_id", "sort_order"})
        }
)
@Check(constraints = "sort_order >= 0")
public class FeedMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false, updatable = false)
    private Feed feed;

    @Column(name = "feed_id", insertable = false, updatable = false)
    private Long feedId;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", insertable = false, updatable = false)
    private Media media;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ", nullable = false, updatable = false)
    private Instant createdAt;

    public static FeedMedia create(Feed feed, Long mediaId, int sortOrder) {
        return FeedMedia.builder()
                .feed(feed)
                .mediaId(mediaId)
                .sortOrder(sortOrder)
                .build();
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
