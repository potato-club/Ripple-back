package org.example.rippleback.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test") // 필요 시 test용 설정 적용
class RedisIntegrationTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void redis_정상_입출력_테스트() {
        // given
        String key = "testKey";
        String value = "testValue";

        // when
        redisTemplate.opsForValue().set(key, value);
        String result = redisTemplate.opsForValue().get(key);

        // then
        assertEquals(value, result);
    }
}