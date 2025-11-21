package org.example.rippleback.features.user.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
import org.example.rippleback.infra.mail.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisTemplate<String, String> redis;
    private final EmailSender emailSender;

    @Value("${app.email.code-ttl-seconds:600}")
    private long codeTtlSeconds;

    @Value("${app.email.cooldown-seconds:30}")
    private long cooldownSeconds;

    private static final SecureRandom RND = new SecureRandom();

    public void sendCode(String email) {
        String em = normalize(email);
        String coolKey = coolKey(em);
        if (Boolean.TRUE.equals(redis.hasKey(coolKey))) return;
        String code = generate6();
        redis.opsForValue().set(codeKey(em), code, Duration.ofSeconds(codeTtlSeconds));
        redis.opsForValue().set(coolKey, "1", Duration.ofSeconds(cooldownSeconds));
        emailSender.send(em, "Ripple 인증 코드", "인증 코드: " + code);
    }

    public void verify(String email, String code) {
        String em = normalize(email);
        String verifiedKey = verifiedKey(em);
        String key = codeKey(em);
        String stored = redis.opsForValue().get(key);
        if (stored == null) {
            if (Boolean.TRUE.equals(redis.hasKey(verifiedKey))) return;
            throw new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED);
        }
        if (!stored.equals(code)) throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID);
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) ttl = codeTtlSeconds;
        redis.delete(key);
        redis.opsForValue().set(verifiedKey, "1", Duration.ofSeconds(ttl));
    }

    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(redis.hasKey(verifiedKey(normalize(email))));
    }

    public void clear(String email) {
        String em = normalize(email);
        redis.delete(codeKey(em));
        redis.delete(verifiedKey(em));
        redis.delete(coolKey(em));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String codeKey(String email) {
        return "ev:code:" + email;
    }

    private static String verifiedKey(String email) {
        return "ev:ok:" + email;
    }

    private static String coolKey(String email) {
        return "ev:cool:" + email;
    }

    private static String generate6() {
        int n = RND.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}
