package org.example.rippleback.core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String corsAllowedMethods;

    @Value("${app.cors.allowed-headers}")
    private String corsAllowedHeaders;

    @Value("${app.cors.exposed-headers:Authorization}")
    private String corsExposedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean corsAllowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowCredentials(corsAllowCredentials);

        var origins = split(corsAllowedOrigins);
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(split(corsAllowedMethods));
        config.setAllowedHeaders(split(corsAllowedHeaders));
        config.setExposedHeaders(split(corsExposedHeaders));
         config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static List<String> split(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
