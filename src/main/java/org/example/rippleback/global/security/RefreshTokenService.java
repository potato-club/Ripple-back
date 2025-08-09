package org.example.rippleback.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    public void save(String email, String refreshToken, long expirationMillis) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        values.set(REFRESH_TOKEN_PREFIX + email, refreshToken, Duration.ofMillis(expirationMillis));
    }

    public String get(String email) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        return values.get(REFRESH_TOKEN_PREFIX + email);
    }

    public void delete(String email) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);
    }

    public boolean exists(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + email));
    }
}
