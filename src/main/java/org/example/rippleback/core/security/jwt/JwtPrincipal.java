package org.example.rippleback.core.security.jwt;

public record JwtPrincipal(Long userId) implements java.security.Principal {
    @Override public String getName() { return String.valueOf(userId); }
}
