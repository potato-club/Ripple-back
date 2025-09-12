package org.example.rippleback.features.media.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = S3ObjectKeyValidator.class)
public @interface S3ObjectKey {
    String message() default "{validation.s3objectkey}";
    int max() default 512;
    boolean allowPrefix() default false;          // (동영상 prefix를 슬래시 없이 받는 정책이면 기본 false로 충분)
    String[] mustStartWith() default {};          // 예: {"users/","posts/"}
    String[] allowedExts() default {};            // 예: {"jpg","jpeg","png","webp","avif"}
    String[] deniedExts() default {};
    int maxSegments() default 0;                  // 과도한 중첩 방지
    int segmentMaxLen() default 0;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
