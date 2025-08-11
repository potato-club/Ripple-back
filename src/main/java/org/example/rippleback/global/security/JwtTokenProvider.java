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
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.time.Clock;

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
    private io.jsonwebtoken.Claims parse(String token) { return Jwts.parserBuilder().setSigningKey(hmacKey).build().parseClaimsJws(token).getBody(); }
    private Key hmacKey;

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
                .claim("typ", "access")
                .claim("ver", tokenVersion)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId, long tokenVersion, String deviceId) {
        Instant now = Instant.now(clock);
        Instant exp = now.plusMillis(refreshTokenExpirationMillis);
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("typ", "refresh")
                .claim("ver", tokenVersion)
                .claim("jti", jti)
                .claim("deviceId", deviceId)
                .setId(jti)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public String getTyp(String token) {
        var v = parse(token).get("typ");
        return v == null ? null : v.toString();
    }

    public long getVersion(String token) {
        var v = parse(token).get("ver");
        return v == null ? 0L : ((Number) v).longValue();
    }

    public String getJti(String token) {
        var v = parse(token).get("jti");
        return v == null ? null : v.toString();
    }

    public String getDeviceId(String token) {
        var v = parse(token).get("deviceId");
        return v == null ? null : v.toString();
    }

    public Instant getExpiration(String token) {
        return parse(token).getExpiration().toInstant();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
