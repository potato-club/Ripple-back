package org.example.rippleback.features.user.infra;

import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.features.user.domain.UserStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<User> findByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    @Query("select u.tokenVersion from User u where u.id = :id")
    Optional<Long> findTokenVersionById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.tokenVersion = u.tokenVersion + 1 where u.id = :id")
    int incrementTokenVersion(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.status = :status, u.deletedAt = :now where u.id = :id and u.deletedAt is null")
    int softDeleteById(@Param("id") Long id, @Param("status") UserStatus status, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.lastLoginAt = :now where u.id = :id")
    int touchLastLogin(@Param("id") Long id, @Param("now") Instant now);
}
