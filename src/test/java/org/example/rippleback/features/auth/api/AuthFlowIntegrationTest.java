package org.example.rippleback.features.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import org.example.rippleback.features.user.domain.User;
import org.example.rippleback.features.user.domain.UserStatus;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

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

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder encoder;

    String email = "u1@ripple.dev";
    String rawPw = "password1!";
    String deviceA = "device-A";
    String deviceB = "device-B";

    @BeforeEach
    void initUser() {
        userRepository.findByEmail(email).ifPresentOrElse(u -> {}, () -> {
            User u = User.builder()
                    .username("u1")
                    .email(email)
                    .password(encoder.encode(rawPw))
                    .status(UserStatus.ACTIVE)
                    .tokenVersion(0L)
                    .build();
            userRepository.save(u);
        });
    }

    @Test
    void login_refresh_rotate_logout_flow_with_error_codes() throws Exception {
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

        var reuseRes = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"device-A"}
                                """.formatted(rt)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        JsonNode reuseJson = om.readTree(reuseRes);
        assertThat(reuseJson.get("code").asText()).isEqualTo("1108");

        var deviceMismatchRes = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"%s"}
                                """.formatted(newRt, deviceB)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        JsonNode deviceMismatchJson = om.readTree(deviceMismatchRes);
        assertThat(deviceMismatchJson.get("code").asText()).isEqualTo("1105");

        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newAt)
                        .header("X-Device-Id", deviceA))
                .andExpect(status().isNoContent());

        var afterLogoutRes = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"device-A"}
                                """.formatted(newRt)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        JsonNode afterLogoutJson = om.readTree(afterLogoutRes);
        assertThat(afterLogoutJson.get("code").asText()).isEqualTo("1106");
    }
}
