package org.example.rippleback.core.security.jwt;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterShouldNotFilterTest {

    @Test
    void should_skip_login_and_refresh() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(null);

        MockHttpServletRequest loginReq = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletRequest refreshReq = new MockHttpServletRequest("POST", "/api/auth/refresh");
        MockHttpServletRequest otherReq = new MockHttpServletRequest("GET", "/api/users/me");

        assertThat(filter.shouldNotFilter(loginReq)).isTrue();
        assertThat(filter.shouldNotFilter(refreshReq)).isTrue();
        assertThat(filter.shouldNotFilter(otherReq)).isFalse();
    }
}
