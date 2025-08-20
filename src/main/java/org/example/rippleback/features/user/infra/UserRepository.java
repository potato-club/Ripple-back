package org.example.rippleback.features.user.infra;

import org.example.rippleback.features.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    @Query("""
            select u
            from User u
            where u.deletedAt is null
              and ( lower(u.username) like lower(concat('%', :q, '%'))
                 or lower(u.email)    like lower(concat('%', :q, '%')) )
              and (:cursorId is null or u.id < :cursorId)
            order by u.id desc
            """)
    List<User> search(@Param("q") String q,
                      @Param("cursorId") Long cursorId,
                      Pageable pageable);

    @Modifying
    @Query("update User u set u.tokenVersion = u.tokenVersion + 1 where u.id = :userId")
    int incrementTokenVersion(@Param("userId") Long userId);
}