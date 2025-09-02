package org.example.rippleback.core.error.exceptions.image;


import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;


import java.util.Map;


public class CorruptedImageDataException extends BusinessException {
    public CorruptedImageDataException(String imageId) {
        super(ErrorCode.CORRUPTED_IMAGE_DATA, Map.of("imageId", imageId));
    }
}