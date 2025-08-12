package org.example.rippleback.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper om;

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e) throws IOException {
        var ec = ErrorCode.COMMON_FORBIDDEN;
        res.setStatus(ec.httpStatus().value());
        res.setContentType("application/json;charset=UTF-8");
        om.writeValue(res.getWriter(), ErrorResponse.of(ec));
    }
}