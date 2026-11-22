package com.pagoflex.usuarios.features.recovery.service.code;

import com.pagoflex.contracts.correo.TipoCodigo;
import com.pagoflex.usuarios.core.cache.RedisExpiringCache;
import com.pagoflex.usuarios.core.cache.RedisTransactionExecutor;
import com.pagoflex.usuarios.features.recovery.exceptions.InvalidRecoveryCode;
import com.pagoflex.usuarios.features.recovery.exceptions.InvalidResetToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceImplTest {
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private RedisOperations<String, String> operations;
    @Mock
    private ValueOperations<String, String> values;

    private VerificationCodeServiceImpl service;

    @BeforeEach
    void setup() {
        RedisTransactionExecutor transactions = new RedisTransactionExecutor(redis);
        service = new VerificationCodeServiceImpl(transactions, new RedisExpiringCache(redis));
        ReflectionTestUtils.setField(service, "codeExpirationMinutes", 10L);
        ReflectionTestUtils.setField(service, "resetExpirationMinutes", 10L);
        lenient().when(redis.execute(ArgumentMatchers.<SessionCallback<Object>>any())).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(operations);
        });
        lenient().when(operations.opsForValue()).thenReturn(values);
        lenient().when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void generateStoresOnlyCodeHashAndExpiration() {
        when(operations.exec()).thenReturn(List.of(1L, 1L, true));

        String code = service.generateCode("user@example.com", TipoCodigo.RECOVERY);

        assertThat(code).matches("[A-Z0-9]{6}");
        verify(operations).multi();
        verify(operations).delete(startsWith("pagoflex:auth:active:"));
        verify(operations).delete(startsWith("pagoflex:auth:attempts:"));
        verify(values).set(startsWith("pagoflex:auth:code:"), argThat(hash ->
                hash.matches("[0-9a-f]{64}") && !hash.equals(code)), eq(Duration.ofSeconds(600)));
        verify(operations).exec();
    }

    @Test
    void verifyRejectsAnExpiredOrPreviouslyConsumedCode() {
        assertThatThrownBy(() -> service.verifyCodeAndIssueResetToken(
                "user@example.com", TipoCodigo.RECOVERY, "ABC123"))
                .isInstanceOf(InvalidRecoveryCode.class);
        verify(operations).unwatch();
        verify(operations).multi();
        verify(values).set(startsWith("pagoflex:auth:attempts:"), eq("1"),
                eq(Duration.ofSeconds(600)));
    }

    @Test
    void verifyIssuesAnOpaqueTokenWithTtlWhenRedisAcceptsTheCode() throws Exception {
        when(values.get(startsWith("pagoflex:auth:code:"))).thenReturn(hash("ABC123"));
        when(values.get(startsWith("pagoflex:auth:attempts:"))).thenReturn("0");
        when(operations.exec()).thenReturn(List.of(1L, 1L, true, true));

        String token = service.verifyCodeAndIssueResetToken(
                "user@example.com", TipoCodigo.RECOVERY, "abc123");

        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        verify(operations).watch(anyList());
        verify(operations).delete(startsWith("pagoflex:auth:code:"));
        verify(operations).delete(startsWith("pagoflex:auth:attempts:"));
        verify(values).set(startsWith("pagoflex:auth:reset:"), eq("user@example.com"),
                eq(Duration.ofSeconds(600)));
        verify(values).set(startsWith("pagoflex:auth:active:"),
                argThat(value -> value.matches("[0-9a-f]{64}")), eq(Duration.ofSeconds(600)));
        verify(operations).exec();
    }

    @Test
    void concurrentVerificationIsRejected() throws Exception {
        when(values.get(startsWith("pagoflex:auth:code:"))).thenReturn(hash("ABC123"));
        when(values.get(startsWith("pagoflex:auth:attempts:"))).thenReturn("0");
        // EXEC devuelve null cuando otra solicitud modifica una clave vigilada.
        assertThatThrownBy(() -> service.verifyCodeAndIssueResetToken(
                "user@example.com", TipoCodigo.RECOVERY, "ABC123"))
                .isInstanceOf(InvalidRecoveryCode.class);
        verify(operations).exec();
    }

    @Test
    void fifthWrongAttemptInvalidatesTheOriginalCode() {
        when(values.get(startsWith("pagoflex:auth:attempts:"))).thenReturn("4");
        when(operations.exec()).thenReturn(List.of(true, true));

        assertThatThrownBy(() -> service.verifyCodeAndIssueResetToken(
                "user@example.com", TipoCodigo.RECOVERY, "WRONG1"))
                .isInstanceOf(InvalidRecoveryCode.class);
        verify(values).set(startsWith("pagoflex:auth:attempts:"), eq("5"),
                eq(Duration.ofSeconds(600)));
        verify(operations).delete(startsWith("pagoflex:auth:code:"));
    }

    @Test
    void correctCodeAfterFiveFailuresIsStillRejected() throws Exception {
        when(values.get(startsWith("pagoflex:auth:attempts:"))).thenReturn("5");

        assertThatThrownBy(() -> service.verifyCodeAndIssueResetToken(
                "user@example.com", TipoCodigo.RECOVERY, "ABC123"))
                .isInstanceOf(InvalidRecoveryCode.class);
        verify(operations, never()).multi();
    }

    @Test
    void rejectsInvalidExpirationBeforeStoringCode() {
        ReflectionTestUtils.setField(service, "codeExpirationMinutes", 0L);
        assertThatThrownBy(() -> service.generateCode("user@example.com", TipoCodigo.RECOVERY))
                .isInstanceOf(IllegalStateException.class);
        verify(redis, never()).execute(ArgumentMatchers.<SessionCallback<Object>>any());
    }

    @Test
    void consumeRejectsBlankToken() {
        assertThatThrownBy(() -> service.consumeResetToken("  "))
                .isInstanceOf(InvalidResetToken.class);
        verify(redis, never()).execute(ArgumentMatchers.<SessionCallback<Object>>any());
    }

    @Test
    void consumeRejectsUnknownToken() {
        assertThatThrownBy(() -> service.consumeResetToken("not-a-token"))
                .isInstanceOf(InvalidResetToken.class);
        verify(operations, never()).multi();
    }

    @Test
    void consumeReturnsEmailOnlyForTheCurrentOneTimeToken() throws Exception {
        when(values.get(startsWith("pagoflex:auth:reset:"))).thenReturn("user@example.com");
        when(values.get(startsWith("pagoflex:auth:active:"))).thenReturn(hash("valid-token"));
        when(operations.exec()).thenReturn(List.of(2L));

        assertThat(service.consumeResetToken("valid-token")).isEqualTo("user@example.com");
        verify(operations).watch(anyList());
        verify(operations).delete(anyList());
        verify(operations).exec();
    }

    @Test
    void consumeRejectsAnAlreadyInvalidatedToken() {
        when(values.get(startsWith("pagoflex:auth:reset:"))).thenReturn("user@example.com");
        when(values.get(startsWith("pagoflex:auth:active:"))).thenReturn("revoked");
        assertThatThrownBy(() -> service.consumeResetToken("expired-token"))
                .isInstanceOf(InvalidResetToken.class);
        verify(operations, never()).multi();
    }

    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
