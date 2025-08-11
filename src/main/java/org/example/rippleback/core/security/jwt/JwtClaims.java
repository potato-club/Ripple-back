package org.example.rippleback.core.security.jwt;

public final class JwtClaims {
    public static final String TOKEN_TYPE = "token_type";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String VERSION = "ver";
    public static final String DEVICE_ID = "deviceId";
    private JwtClaims() {}
}
