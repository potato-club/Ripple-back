package org.example.rippleback.core.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.exceptions.auth.TokenExpiredException;
import org.example.rippleback.core.error.exceptions.auth.TokenInvalidException;
import org.example.rippleback.core.error.exceptions.auth.TokenTypeInvalidException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.example.rippleback.core.security.jwt.JwtClaims.TOKEN_TYPE;
import static org.example.rippleback.core.security.jwt.JwtClaims.TOKEN_TYPE_ACCESS;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private static final Set<String> SKIP_PATHS = Set.of("/api/auth/login", "/api/auth/refresh");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        return SKIP_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                var c = jwtTokenProvider.decode(token);
                if (TOKEN_TYPE_ACCESS.equals(c.tokenType())) {
                    var principal = new JwtPrincipal(c.userId());
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    request.setAttribute("auth_error", new TokenTypeInvalidException());
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                request.setAttribute("auth_error", new TokenExpiredException());
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                request.setAttribute("auth_error", new TokenInvalidException());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
