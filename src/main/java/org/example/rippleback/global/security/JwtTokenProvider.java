package org.example.rippleback.global.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMillis;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMillis;

    private final Clock clock;
    private Key hmacKey;

    public static record TokenClaims(
            Long userId, String tokenType, Long version, String jti, String deviceId, Instant exp
    ) {}

    @PostConstruct
    public void init() {
        byte[] keyBytes = Objects.requireNonNull(secretKey, "jwt.secret must not be null")
                .getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes for HS256");
        }
        this.hmacKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, long tokenVersion) {
        Instant now = Instant.now(clock);
        Instant exp = now.plusMillis(accessTokenExpirationMillis);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("token_type", "access")
                .claim("ver", tokenVersion)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId, long tokenVersion, String deviceId) {
        Instant now = Instant.now(clock);
        Instant exp = now.plusMillis(refreshTokenExpirationMillis);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("token_type", "refresh")
                .claim("ver", tokenVersion)
                .claim("deviceId", deviceId)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public TokenClaims decode(String token) {
        var jws = Jwts.parserBuilder()
                .setSigningKey(hmacKey)
                .setAllowedClockSkewSeconds(60)
                .build()
                .parseClaimsJws(token);
        var c = jws.getBody();
        Long userId = Long.valueOf(c.getSubject());
        String tokenType = (String) c.get("token_type");
        Long version = c.get("ver", Number.class) == null
                ? null : c.get("ver", Number.class).longValue();
        return new TokenClaims(
                userId,
                tokenType,
                version,
                c.getId(),
                (String) c.get("deviceId"),
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
