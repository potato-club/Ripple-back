package org.example.rippleback.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.*;
import org.example.rippleback.core.error.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper om;

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        ErrorCode ec;
        Object attr = req.getAttribute("auth_error");
        if (attr instanceof BusinessException be) {
            ec = be.errorCode();
        } else {
            String h = req.getHeader(HttpHeaders.AUTHORIZATION);
            ec = (h == null || !h.startsWith("Bearer ")) ? ErrorCode.TOKEN_MISSING : ErrorCode.COMMON_UNAUTHORIZED;
        }
        res.setStatus(ec.httpStatus().value());
        res.setContentType("application/json;charset=UTF-8");
        om.writeValue(res.getWriter(), ErrorResponse.of(ec));
    }
}
