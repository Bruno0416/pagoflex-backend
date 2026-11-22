package com.pagoflex.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RedisAccessTokenValidatorTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock();
    private final RedisAccessTokenValidator validator = new RedisAccessTokenValidator(redis);
    private final String uid = UUID.randomUUID().toString();
    private final String sid = UUID.randomUUID().toString();
    private final String jti = UUID.randomUUID().toString();

    private Jwt jwt() {
        return Jwt.withTokenValue("jwt").header("alg", "HS256")
                .subject(uid).claim("jti", jti).claim("sid", sid).build();
    }

    // ---------- tokens de acceso vigentes ----------
    @Test
    void acceptsOnlyAccessLinkedToCurrentSession() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:access:" + jti)).thenReturn(sid);
        when(values.get("auth:session:" + sid)).thenReturn(uid + "|" + jti + "|sha256");
        assertThat(validator.validate(jwt()).hasErrors()).isFalse();
    }

    @Test
    void rejectsRevokedAccess() {
        when(redis.opsForValue()).thenReturn(values);
        assertThat(validator.validate(jwt()).hasErrors()).isTrue();
    }

    @Test
    void rejectsSessionWithDifferentUser() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:access:" + jti)).thenReturn(sid);
        when(values.get("auth:session:" + sid)).thenReturn(UUID.randomUUID() + "|" + jti + "|sha256");
        assertThat(validator.validate(jwt()).hasErrors()).isTrue();
    }

    @Test
    void rejectsSessionWithOlderAccessId() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:access:" + jti)).thenReturn(sid);
        when(values.get("auth:session:" + sid)).thenReturn(uid + "|" + UUID.randomUUID() + "|sha256");
        assertThat(validator.validate(jwt()).hasErrors()).isTrue();
    }

    @Test
    void failsClosedWhenRedisThrows() {
        when(redis.opsForValue()).thenThrow(new IllegalStateException("redis caido"));
        assertThat(validator.validate(jwt()).hasErrors()).isTrue();
    }

    @Test
    void rejectsMalformedSessionIdWithoutCallingRedis() {
        Jwt invalid = Jwt.withTokenValue("jwt").header("alg", "HS256")
                .subject(uid).claim("jti", jti).claim("sid", "not-a-uuid").build();
        assertThat(validator.validate(invalid).hasErrors()).isTrue();
        verifyNoInteractions(redis);
    }
}
