package org.example.rippleback.features.user.infra;

import org.example.rippleback.features.user.domain.User;

import java.util.Optional;

public interface UserRepository {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}