package org.example.rippleback.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.cookie")
public record AuthCookieProperties(
        String name,
        String path,
        String sameSite,
        boolean secure,
        boolean httpOnly,
        String domain
) {}
