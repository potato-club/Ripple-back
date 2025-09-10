package org.example.rippleback.features.media.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

@Component
public class MediaUrlResolver {
    private final String base; // trailing '/' 제거된 CDN base

    public MediaUrlResolver(@Value("${cdn.base-url}") String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("cdn.base-url must be set");
        }
        // 끝 슬래시 제거 (여러 개도 제거)
        this.base = baseUrl.replaceAll("/+$", "");
    }

    /**
     * DB에 저장된 object key -> 공개 URL (key가 비어있으면 null)
     * key는 검증기(@S3ObjectKey)를 통과한다고 가정
     */
    public String toPublicUrl(String key) {
        if (key == null || key.isBlank()) return null;
        String[] segments = key.split("/"); // 각 세그먼트를 안전하게 인코딩하여 조립
        return UriComponentsBuilder.fromHttpUrl(base)
                .path("/")
                .pathSegment(segments)
                .build()                // 각 segment를 필요시 인코딩
                .toUriString();
    }

    /* =========================
       동영상 prefix 전용 헬퍼
       ========================= */

    /** HLS 재생 manifest URL: {assetPrefix}/hls/index.m3u8 */
    public String hlsManifestUrl(String assetPrefix) {
        if (assetPrefix == null || assetPrefix.isBlank()) return null;
        return toPublicUrl(assetPrefix + "/hls/index.m3u8");
    }

    /** 원본(예: mp4) URL: {assetPrefix}/source.{ext} */
    public String videoSourceUrl(String assetPrefix, String ext) {
        if (assetPrefix == null || assetPrefix.isBlank() || ext == null || ext.isBlank()) return null;
        return toPublicUrl(assetPrefix + "/source." + ext);
    }

    /** 썸네일 정해진 프레임 URL: {assetPrefix}/thumb/0001.jpg */
    public String videoThumbUrl(String assetPrefix, int frame) {
        if (assetPrefix == null || assetPrefix.isBlank()) return null;
        int f = Math.max(0, frame);
        return toPublicUrl(assetPrefix + "/thumb/" + String.format("%04d", f) + ".jpg");
    }

    /* =========================
       (선택) 이미지 변형/버전 파생 키 조립
       예: {keyWithoutExt}@2x.jpg, {key}?w=1080 등 규칙이 있다면 여기에
       ========================= */
    public String imageVariantUrl(String key, String suffixOrQuery) {
        if (key == null || key.isBlank()) return null;
        Objects.requireNonNull(suffixOrQuery, "suffixOrQuery");
        // 쿼리 파라미터 방식이면 그대로 붙이고, 접미사 방식이면 key 가공 규칙에 맞춰 변형
        if (suffixOrQuery.startsWith("?")) {
            return toPublicUrl(key) + suffixOrQuery;
        }
        // 접미사 방식 예시: photo.jpg -> photo@2x.jpg
        int dot = key.lastIndexOf('.');
        String variantKey = (dot > 0)
                ? key.substring(0, dot) + suffixOrQuery + key.substring(dot)
                : key + suffixOrQuery;
        return toPublicUrl(variantKey);
    }

    /* Getter (테스트용) */
    String getBase() { return base; }
}