package org.example.rippleback.features.auth.api.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.rippleback.core.config.AuthCookieProperties;
import org.example.rippleback.core.security.jwt.JwtTokenProvider;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class RefreshCookieSupport {
    private final AuthCookieProperties props;
    private final JwtTokenProvider jwt;   // ← 토큰 디코드용
    private final Clock clock;            // ← 테스트 용이성/일관성

    public RefreshCookieSupport(AuthCookieProperties props, JwtTokenProvider jwt, Clock clock) {
        this.props = props;
        this.jwt = jwt;
        this.clock = clock;
    }

    /** RT에서 exp를 읽어 Max-Age를 자동 계산해 Set-Cookie 작성 */
    public void writeFromToken(HttpServletResponse res, String refreshToken) {
        var c = jwt.decode(refreshToken);
        long maxAge = Math.max(1, Duration.between(Instant.now(clock), c.exp()).getSeconds());
        write(res, refreshToken, maxAge);
    }

    /** Max-Age를 외부에서 넘겨줄 때 사용할 버전(유지) */
    public void write(HttpServletResponse res, String refreshToken, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(props.name(), refreshToken)
                .httpOnly(props.httpOnly())
                .secure(props.secure())
                .path(props.path())
                .maxAge(Duration.ofSeconds(maxAgeSeconds));
        if (props.domain() != null && !props.domain().isBlank()) b.domain(props.domain());
        String setCookie = b.build().toString() + "; SameSite=" + props.sameSite();
        res.addHeader("Set-Cookie", setCookie);
    }

    public void clear(HttpServletResponse res) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(props.name(), "")
                .httpOnly(props.httpOnly())
                .secure(props.secure())
                .path(props.path())
                .maxAge(Duration.ZERO);
        if (props.domain() != null && !props.domain().isBlank()) b.domain(props.domain());
        String setCookie = b.build().toString() + "; SameSite=" + props.sameSite();
        res.addHeader("Set-Cookie", setCookie);
    }

    public String read(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        for (var c : req.getCookies()) {
            if (props.name().equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
