package org.example.rippleback.security;

import org.example.rippleback.infra.redis.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers
class RefreshTokenServiceTest {

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    RefreshTokenService service;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Test
    void store_get_delete_per_device() {
        Long uid = 10L;
        String dev1 = "d1";
        String dev2 = "d2";
        service.store(uid, dev1, "jti-1", "hash-1", 30000);
        service.store(uid, dev2, "jti-2", "hash-2", 30000);
        Optional<RefreshTokenService.RefreshEntry> e1 = service.get(uid, dev1);
        Optional<RefreshTokenService.RefreshEntry> e2 = service.get(uid, dev2);
        assertThat(e1).isPresent();
        assertThat(e2).isPresent();
        assertThat(e1.get().jti()).isEqualTo("jti-1");
        assertThat(e2.get().jti()).isEqualTo("jti-2");
        service.delete(uid, dev1);
        assertThat(service.get(uid, dev1)).isEmpty();
        assertThat(service.get(uid, dev2)).isPresent();
        Set<String> membersAfterDelete = redisTemplate.opsForSet().members("rtdev:" + uid);
        assertThat(membersAfterDelete).containsExactly("d2");
        service.deleteAll(uid);
        assertThat(service.get(uid, dev2)).isEmpty();
        Set<String> membersAfterAll = redisTemplate.opsForSet().members("rtdev:" + uid);
        assertThat(membersAfterAll == null || membersAfterAll.isEmpty()).isTrue();
    }

    @Test
    void reuse_mark_and_check() {
        service.markUsed("jti-x", 2000);
        boolean used = service.isUsed("jti-x");
        assertThat(used).isTrue();
    }
}
