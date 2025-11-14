package org.example.rippleback.core.error.exceptions.auth;


import lombok.Getter;
import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;


import java.util.Map;


@Getter
public class DeviceMismatchException extends BusinessException {
    private final String expectedDevice;
    private final String actualDevice;


    public DeviceMismatchException(String expectedDevice, String actualDevice) {
        super(ErrorCode.DEVICE_MISMATCH, Map.of("expectedDevice", expectedDevice, "actualDevice", actualDevice));
        this.expectedDevice = expectedDevice;
        this.actualDevice = actualDevice;
    }


}