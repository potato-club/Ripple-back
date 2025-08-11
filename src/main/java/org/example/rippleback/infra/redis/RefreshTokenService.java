package org.example.rippleback.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String RT_KEY_PREFIX = "rt:";
    private static final String RJTI_KEY = "rjti:";
    private static final String DEVICES_KEY_PREFIX = "rtdev:";

    public record RefreshEntry(String jti, String hash) {
    }

    private String rtKey(Long userId, String deviceId) {
        return RT_KEY_PREFIX + userId + ":" + deviceId;
    }

    private String devicesKey(Long userId) {
        return DEVICES_KEY_PREFIX + userId;
    }

    public void store(Long userId, String deviceId, String jti, String hash, long ttlMillis) {
        String key = rtKey(userId, deviceId);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        ops.putAll(key, Map.of("jti", jti, "hash", hash));
        redisTemplate.expire(key, Duration.ofMillis(ttlMillis));
        redisTemplate.opsForSet().add(devicesKey(userId), deviceId);
    }

    public Optional<RefreshEntry> get(Long userId, String deviceId) {
        String key = rtKey(userId, deviceId);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String jti = ops.get(key, "jti");
        String hash = ops.get(key, "hash");
        if (jti == null || hash == null) return Optional.empty();
        return Optional.of(new RefreshEntry(jti, hash));
    }

    public void delete(Long userId, String deviceId) {
        redisTemplate.delete(rtKey(userId, deviceId));
        redisTemplate.opsForSet().remove(devicesKey(userId), deviceId);
    }

    public void deleteAll(Long userId) {
        String dkey = devicesKey(userId);
        Set<String> devices = redisTemplate.opsForSet().members(dkey);
        if (devices != null && !devices.isEmpty()) {
            devices.forEach(dev -> redisTemplate.delete(rtKey(userId, dev)));
        }
        redisTemplate.delete(dkey);
    }

    public void markUsed(String jti, long ttlMillis) {
        redisTemplate.opsForValue().set(RJTI_KEY + jti, "1", Duration.ofMillis(ttlMillis));
    }

    public boolean isUsed(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RJTI_KEY + jti));
    }
}
