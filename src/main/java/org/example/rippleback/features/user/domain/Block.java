package org.example.rippleback.features.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "user_block",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_block", columnNames = {"from_user_id","to_user_id"}))
public class Block {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "from_user_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "to_user_id", nullable = false)
    private User blocked;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}