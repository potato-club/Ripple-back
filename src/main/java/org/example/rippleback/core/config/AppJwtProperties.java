package org.example.rippleback.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.jwt")
public record AppJwtProperties(
        @DefaultValue("120") long allowedClockSkewSeconds
) {}