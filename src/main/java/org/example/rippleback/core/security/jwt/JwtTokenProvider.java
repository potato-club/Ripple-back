package org.example.rippleback.core.security.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import static org.example.rippleback.core.security.jwt.JwtClaims.*;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties props;
    private final Clock clock;
    private Key hmacKey;

    public static record TokenClaims(
            Long userId, String tokenType, Long version, String jti, String deviceId, Instant exp
    ) {}

    @PostConstruct
    public void init() {
        byte[] keyBytes = Objects.requireNonNull(props.secret(), "jwt.secret must not be null")
                .getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes for HS256");
        }
        this.hmacKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, long tokenVersion) {
        Instant now = Instant.now(clock);
        Instant exp = now.plusMillis(props.accessTokenExpiration());
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .claim(VERSION, tokenVersion)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId, long tokenVersion, String deviceId) {
        Instant now = Instant.now(clock);
        Instant exp = now.plusMillis(props.refreshTokenExpiration());
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .claim(VERSION, tokenVersion)
                .claim(DEVICE_ID, deviceId)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public TokenClaims decode(String token) {
        var jws = Jwts.parserBuilder()
                .setSigningKey(hmacKey)
                .setAllowedClockSkewSeconds(props.allowedClockSkewSeconds())
                .build()
                .parseClaimsJws(token);
        var c = jws.getBody();
        Long userId = Long.valueOf(c.getSubject());
        String tokenType = (String) c.get(TOKEN_TYPE);
        Long version = c.get(VERSION, Number.class) == null
                ? null : c.get(VERSION, Number.class).longValue();
        return new TokenClaims(
                userId,
                tokenType,
                version,
                c.getId(),
                (String) c.get(DEVICE_ID),
                c.getExpiration().toInstant()
        );
    }

    public boolean isValid(String token) {
        try {
            decode(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
