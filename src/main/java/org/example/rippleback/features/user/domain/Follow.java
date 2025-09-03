package org.example.rippleback.features.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "user_follow",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_follow", columnNames = {"from_user_id", "to_user_id"}),
        indexes = {
                @Index(name = "idx_follow_from", columnList = "from_user_id"),
                @Index(name = "idx_follow_to", columnList = "to_user_id")
        }
)
@Check(constraints = "from_user_id <> to_user_id")
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User follower;

    @Column(name = "from_user_id", insertable = false, updatable = false)
    private Long followerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User following;

    @Column(name = "to_user_id", insertable = false, updatable = false)
    private Long followingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}