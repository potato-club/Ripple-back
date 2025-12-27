package org.example.rippleback.features.media.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

@Component
public class MediaUrlResolver {
    private final String base;

    public MediaUrlResolver(@Value("${CDN_BASE_URL}") String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("cdn.base-url must be set");
        }
        this.base = baseUrl.replaceAll("/+$", "");
    }

    public String toPublicUrl(String key) {
        if (key == null || key.isBlank()) return null;
        String[] segments = key.split("/");
        return UriComponentsBuilder.fromHttpUrl(base)
                .path("/")
                .pathSegment(segments)
                .build()
                .toUriString();
    }

    public String hlsManifestUrl(String assetPrefix) {
        if (assetPrefix == null || assetPrefix.isBlank()) return null;
        return toPublicUrl(assetPrefix + "/hls/index.m3u8");
    }

    public String videoSourceUrl(String assetPrefix, String ext) {
        if (assetPrefix == null || assetPrefix.isBlank() || ext == null || ext.isBlank()) return null;
        return toPublicUrl(assetPrefix + "/source." + ext);
    }

    public String videoThumbUrl(String assetPrefix, int frame) {
        if (assetPrefix == null || assetPrefix.isBlank()) return null;
        int f = Math.max(0, frame);
        return toPublicUrl(assetPrefix + "/thumb/" + String.format("%04d", f) + ".jpg");
    }

    public String imageVariantUrl(String key, String suffixOrQuery) {
        if (key == null || key.isBlank()) return null;
        Objects.requireNonNull(suffixOrQuery, "suffixOrQuery");
        if (suffixOrQuery.startsWith("?")) {
            return toPublicUrl(key) + suffixOrQuery;
        }
        int dot = key.lastIndexOf('.');
        String variantKey = (dot > 0)
                ? key.substring(0, dot) + suffixOrQuery + key.substring(dot)
                : key + suffixOrQuery;
        return toPublicUrl(variantKey);
    }

    String getBase() { return base; }
}