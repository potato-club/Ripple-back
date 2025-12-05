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
    boolean allowPrefix() default false;
    String[] mustStartWith() default {};
    String[] allowedExts() default {};
    String[] deniedExts() default {};
    int maxSegments() default 0;
    int segmentMaxLen() default 0;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
