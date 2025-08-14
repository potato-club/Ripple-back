package org.example.rippleback.features.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_users_created_at", columnList = "created_at"),
                @Index(name = "idx_users_last_login_at", columnList = "last_login_at")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String username;

    @Column(length = 100, nullable = false)
    private String email;

    @Column(length = 255, nullable = false)
    private String password;

    @Column(name = "profile_image_url", columnDefinition = "text")
    private String profileImageUrl;

    @Column(name = "profile_message", length = 255)
    private String profileMessage;

    @Builder.Default
    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @Column(name = "token_version", nullable = false)
    private Long tokenVersion = 0L;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void updateProfile(String newUsername, String newMessage, String newImageUrl) {
        if (newUsername != null && !newUsername.isBlank()) this.username = newUsername;
        this.profileMessage = newMessage;
        this.profileImageUrl = newImageUrl;
    }

    public void touchLastLogin(Instant now) {
        this.lastLoginAt = now;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void softDelete(Instant now) {
        this.status = UserStatus.DELETED;
        this.deletedAt = now;
    }

    public void bumpTokenVersion() {
        this.tokenVersion = this.tokenVersion + 1;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
