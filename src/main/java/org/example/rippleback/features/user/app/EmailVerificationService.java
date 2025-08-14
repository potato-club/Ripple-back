package org.example.rippleback.features.user.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.exceptions.user.EmailCodeExpiredException;
import org.example.rippleback.core.error.exceptions.user.EmailCodeInvalidException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "email:ver:";
    private static final Duration TTL = Duration.ofMinutes(10);

    public String sendCode(String email) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        String key = KEY_PREFIX + email.toLowerCase(Locale.ROOT);
        redisTemplate.opsForValue().set(key, code, TTL);
        return code;
    }

    public void verify(String email, String code) {
        String key = KEY_PREFIX + email.toLowerCase(Locale.ROOT);
        String saved = redisTemplate.opsForValue().get(key);
        if (saved == null) throw new EmailCodeExpiredException();
        if (!saved.equals(code)) throw new EmailCodeInvalidException();
        redisTemplate.delete(key);
    }
}