package org.example.rippleback.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.domain.user.entity.UserStatus;
import org.example.rippleback.features.user.infra.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", redis::getFirstMappedPort);
        r.add("jwt.secret", () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        r.add("jwt.access-token-expiration", () -> 60_000);
        r.add("jwt.refresh-token-expiration", () -> 3_600_000);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    String email = "u1@ripple.dev";
    String rawPw = "password1!";
    String deviceA = "device-A";
    String deviceB = "device-B";

    @BeforeEach
    void initUser() {
        userRepository.findByEmail(email).ifPresentOrElse(u -> {}, () -> {
            User u = new User();
            u.setUserId("u1");
            u.setEmail(email);
            u.setPassword(encoder.encode(rawPw));
            u.setStatus(UserStatus.ACTIVE);
            u.setTokenVersion(0L);
            u.setCreatedAt(Instant.now());
            userRepository.save(u);
        });
    }

    @Test
    void login_refresh_rotate_logout_flow() throws Exception {
        var loginRes = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"u1@ripple.dev","password":"password1!","deviceId":"device-A"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode loginJson = om.readTree(loginRes);
        String at = loginJson.get("accessToken").asText();
        String rt = loginJson.get("refreshToken").asText();
        assertThat(at).isNotBlank();
        assertThat(rt).isNotBlank();

        var refRes = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"device-A"}
                                """.formatted(rt)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode refJson = om.readTree(refRes);
        String newAt = refJson.get("accessToken").asText();
        String newRt = refJson.get("refreshToken").asText();
        assertThat(newAt).isNotBlank();
        assertThat(newRt).isNotBlank();
        assertThat(newRt).isNotEqualTo(rt);

        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"device-A"}
                                """.formatted(rt)))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"device-B"}
                                """.formatted(newRt)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newAt)
                        .header("X-Device-Id", deviceA))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"device-A"}
                                """.formatted(newRt)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_from_two_devices_then_logoutAll_invalidates_both() throws Exception {
        var loginA = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","deviceId":"%s"}
                                """.formatted(email, rawPw, deviceA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var jsonA = om.readTree(loginA);
        String atA = jsonA.get("accessToken").asText();
        String rtA = jsonA.get("refreshToken").asText();

        var loginB = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","deviceId":"%s"}
                                """.formatted(email, rawPw, deviceB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var jsonB = om.readTree(loginB);
        String atB = jsonB.get("accessToken").asText();
        String rtB = jsonB.get("refreshToken").asText();

        mvc.perform(post("/api/auth/logout/all")
                        .header("Authorization", "Bearer " + atB))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"%s"}
                                """.formatted(rtA, deviceA)))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"%s"}
                                """.formatted(rtB, deviceB)))
                .andExpect(status().isUnauthorized());
    }
}
