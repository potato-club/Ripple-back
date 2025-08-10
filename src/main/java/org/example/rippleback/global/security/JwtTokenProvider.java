package org.example.rippleback.global.security;

import io.jsonwebtoken.Claims;
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

    private Key hmacKey;

    @PostConstruct
    public void init() {
        this.hmacKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 발급
     */
    public String createAccessToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMillis);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token 발급 (랜덤 UUID) + Redis 저장
     */
    public String createRefreshToken(String email) {
        String refreshToken = UUID.randomUUID().toString();
        return refreshToken;
    }

    /**
     * Token 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(hmacKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰에서 이메일(Subject) 추출
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(hmacKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * AT: sub=userId, typ=access, ver, iat/exp
     */
    public String createAccessToken(Long userId, long tokenVersion) {
        Instant now = Instant.now();
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

    /**
     * RT(JWT): sub=userId, typ=refresh, ver, jti, iat/exp
     */
    public String createRefreshToken(Long userId, long tokenVersion) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(refreshTokenExpirationMillis);
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("typ", "refresh")
                .claim("ver", tokenVersion)
                .claim("jti", jti)
                .setId(jti) // 표준 jti도 함께 설정(옵션)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ---------- Parser Utilities ----------
    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(hmacKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
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

    public Instant getExpiration(String token) {
        return parse(token).getExpiration().toInstant();
    }

    /**
     * 신규 명칭의 유효성 체크
     */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
