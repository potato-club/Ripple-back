package org.example.rippleback.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    // 화이트리스트: rt:<userId> -> Hash { jti, hash } (+ TTL)
    // 재사용 마킹: rjti:<jti> -> "1" (+ TTL: 기존 RT 남은 만료)
    private static final String RT_KEY = "rt:";
    private static final String RJTI_KEY = "rjti:";

    public record RefreshEntry(String jti, String hash) {
    }

    /**
     * 화이트리스트 저장: rt:<userId> = {jti, hash} (TTL=남은 만료)
     */
    public void store(Long userId, String jti, String hash, long ttlMillis) {
        String key = RT_KEY + userId;
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        ops.putAll(key, Map.of("jti", jti, "hash", hash));
        redisTemplate.expire(key, Duration.ofMillis(ttlMillis));
    }

    /**
     * 화이트리스트 조회
     */
    public Optional<RefreshEntry> get(Long userId) {
        String key = RT_KEY + userId;
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String jti = ops.get(key, "jti");
        String hash = ops.get(key, "hash");
        if (jti == null || hash == null) return Optional.empty();
        return Optional.of(new RefreshEntry(jti, hash));
    }

    /**
     * 화이트리스트 제거(로그아웃 등)
     */
    public void delete(Long userId) {
        redisTemplate.delete(RT_KEY + userId);
    }

    /**
     * 재사용 마킹(rjti:<jti>=1, TTL=기존 RT 남은 만료)
     */
    public void markUsed(String jti, long ttlMillis) {
        redisTemplate.opsForValue().set(RJTI_KEY+ jti, "1", Duration.ofMillis(ttlMillis));
    }

    /**
     * 재사용 여부 확인
     */
    public boolean isUsed(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RJTI_KEY + jti));
    }
}