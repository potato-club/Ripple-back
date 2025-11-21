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
    private final JwtTokenProvider jwt;
    private final Clock clock;

    public RefreshCookieSupport(AuthCookieProperties props, JwtTokenProvider jwt, Clock clock) {
        this.props = props;
        this.jwt = jwt;
        this.clock = clock;
    }

    public void writeFromToken(HttpServletResponse res, String refreshToken) {
        var c = jwt.decode(refreshToken);
        long maxAge = Math.max(1, Duration.between(Instant.now(clock), c.exp()).getSeconds());
        write(res, refreshToken, maxAge);
    }

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
