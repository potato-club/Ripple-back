package org.example.rippleback.features.feed.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.rippleback.features.user.domain.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"feed_id", "user_id"})
})
public class FeedLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @Column(nullable = false)
    private Long userId;
}
