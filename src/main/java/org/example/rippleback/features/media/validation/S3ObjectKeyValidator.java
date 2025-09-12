package org.example.rippleback.features.media.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class S3ObjectKeyValidator implements ConstraintValidator<S3ObjectKey, String> {

    private int max;
    private boolean allowPrefix;
    private String[] mustStartWith;
    private Set<String> allowedExts;
    private Set<String> deniedExts;

    // 첫 글자 영숫자, 이후 허용 문자만
    private static final Pattern SAFE =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9/_\\-.]{0,1023}$");
    // ./, ../, // 패턴 차단
    private static final Pattern UNSAFE =
            Pattern.compile("(?:^|/)\\.(?:\\.|/|$)|//");

    @Override
    public void initialize(S3ObjectKey ann) {
        this.max = ann.max();
        this.allowPrefix = ann.allowPrefix();
        this.mustStartWith = ann.mustStartWith();
        this.allowedExts = toLowerSet(ann.allowedExts());
        this.deniedExts = toLowerSet(ann.deniedExts());
    }

    @Override
    public boolean isValid(String key, ConstraintValidatorContext ctx) {
        if (key == null || key.isBlank()) return true; // 필수 여부는 @NotBlank로

        // 길이(문자/바이트) 제한
        if (key.length() > max || key.getBytes(StandardCharsets.UTF_8).length > max) return false;

        // 허용 문자 집합
        if (!SAFE.matcher(key).matches()) return false;

        // 위험 시퀀스
        if (UNSAFE.matcher(key).find()) return false;

        // 트레일링 슬래시
        if (!allowPrefix && key.endsWith("/")) return false;

        // 접두사 제약
        if (mustStartWith != null && mustStartWith.length > 0) {
            boolean ok = Arrays.stream(mustStartWith).anyMatch(key::startsWith);
            if (!ok) return false;
        }

        // 확장자 화이트/블랙 리스트
        String ext = extOf(key);
        if (!allowedExts.isEmpty() && (ext == null || !allowedExts.contains(ext))) return false;
        if (!deniedExts.isEmpty() && ext != null && deniedExts.contains(ext)) return false;

        return true;
    }

    private static Set<String> toLowerSet(String[] arr) {
        Set<String> s = new HashSet<>();
        if (arr != null) for (String a : arr) if (a != null && !a.isBlank()) s.add(a.toLowerCase());
        return s;
    }

    private static String extOf(String key) {
        int slash = key.lastIndexOf('/');
        String name = (slash >= 0) ? key.substring(slash + 1) : key;
        int dot = name.lastIndexOf('.');
        return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1).toLowerCase() : null;
    }
}
