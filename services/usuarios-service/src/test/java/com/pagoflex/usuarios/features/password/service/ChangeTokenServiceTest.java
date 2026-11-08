package com.pagoflex.usuarios.features.password.service;

import com.pagoflex.usuarios.core.cache.ExpiringCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeTokenServiceTest {
    @Mock
    private ExpiringCache cache;

    @Test
    void issuesOpaqueTokenAndStoresOnlyFingerprints() {
        UUID userId = UUID.randomUUID();
        ChangeTokenService service = new ChangeTokenService(cache);
        String token = service.issueToken(userId, "bcrypt-hash");

        assertThat(service.expiresInSeconds()).isEqualTo(300);
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(cache).put(key.capture(), value.capture(), eq(Duration.ofMinutes(5)));
        assertThat(key.getValue()).startsWith("pagoflex:auth:password-change:" + userId + ":")
                .doesNotContain(token);
        assertThat(value.getValue()).matches("[0-9a-f]{64}").isNotEqualTo("bcrypt-hash");
    }

    @Test
    void bindsTokenToUserAndPasswordVersionAndConsumesOnce() {
        UUID userId = UUID.randomUUID();
        ChangeTokenService service = new ChangeTokenService(cache);
        String token = service.issueToken(userId, "first-hash");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(cache).put(key.capture(), value.capture(), eq(Duration.ofMinutes(5)));
        var consumed = new java.util.concurrent.atomic.AtomicBoolean(false);
        when(cache.consumeIfMatches(anyString(), anyString())).thenAnswer(invocation ->
                key.getValue().equals(invocation.getArgument(0))
                        && value.getValue().equals(invocation.getArgument(1))
                        && consumed.compareAndSet(false, true));

        assertThat(service.consumeToken(UUID.randomUUID(), token, "first-hash")).isFalse();
        assertThat(service.consumeToken(userId, token, "changed-hash")).isFalse();
        assertThat(service.consumeToken(userId, token, "first-hash")).isTrue();
        assertThat(service.consumeToken(userId, token, "first-hash")).isFalse();
    }

    @Test
    void deniesEmptyAndMalformedTokenWithoutRedisCall() {
        ChangeTokenService service = new ChangeTokenService(cache);
        UUID userId = UUID.randomUUID();
        assertThat(service.consumeToken(userId, "", "hash")).isFalse();
        assertThat(service.consumeToken(userId, "bad", "hash")).isFalse();
        verifyNoInteractions(cache);
    }
}
