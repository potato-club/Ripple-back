package org.example.rippleback.features.feed.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;



@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "feed_view_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "feed_id"}))
public class FeedViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long feedId;

    private Instant viewedAt;
}
