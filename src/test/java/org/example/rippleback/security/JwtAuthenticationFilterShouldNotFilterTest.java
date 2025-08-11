package org.example.rippleback.security;

import jakarta.servlet.http.HttpServletRequest;
import org.example.rippleback.core.security.jwt.JwtAuthenticationFilter;
import org.example.rippleback.core.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterShouldNotFilterTest {

    static class ExposedFilter extends JwtAuthenticationFilter {
        public ExposedFilter(JwtTokenProvider p) { super(p); }
        public boolean callShouldNotFilter(HttpServletRequest req) { return super.shouldNotFilter(req); }
    }

    @Test
    void should_skip_for_auth_paths_and_options() {
        var provider = Mockito.mock(JwtTokenProvider.class);
        var filter = new ExposedFilter(provider);

        var req1 = new MockHttpServletRequest("GET", "/api/auth/login");
        assertThat(filter.callShouldNotFilter(req1)).isTrue();

        var req2 = new MockHttpServletRequest("OPTIONS", "/api/secure/me");
        assertThat(filter.callShouldNotFilter(req2)).isTrue();

        var req3 = new MockHttpServletRequest("GET", "/api/secure/me");
        assertThat(filter.callShouldNotFilter(req3)).isFalse();
    }
}
