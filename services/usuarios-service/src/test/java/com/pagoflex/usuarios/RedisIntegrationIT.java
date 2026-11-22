package com.pagoflex.usuarios;

import com.pagoflex.contracts.correo.TipoCodigo;
import com.pagoflex.security.RedisAccessTokenValidator;
import com.pagoflex.usuarios.core.cache.RedisExpiringCache;
import com.pagoflex.usuarios.core.cache.RedisTransactionExecutor;
import com.pagoflex.usuarios.core.security.JwtUtil;
import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidSession;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.auth.session.RedisAuthSessionService;
import com.pagoflex.usuarios.features.recovery.exceptions.InvalidRecoveryCode;
import com.pagoflex.usuarios.features.recovery.exceptions.InvalidResetToken;
import com.pagoflex.usuarios.features.recovery.service.code.RecoverySendThrottle;
import com.pagoflex.usuarios.features.recovery.service.code.VerificationCodeServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class RedisIntegrationIT {
    private static final String SECRET = "it-only-jwt-secret-that-has-at-least-32-bytes";

    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;

    private final UserRepository users = mock(UserRepository.class);
    private final JwtUtil jwt = new JwtUtil(SECRET, Duration.ofMinutes(15));
    private RedisAuthSessionService sessions;
    private RedisTransactionExecutor transactions;
    private RedisExpiringCache cache;
    private VerificationCodeServiceImpl codes;
    private RedisAccessTokenValidator validator;
    private NimbusJwtDecoder decoder;
    private Users user;

    @BeforeAll
    static void connect() {
        String port = System.getProperty("pagoflex.it.redis.port");
        if (port == null) {
            throw new IllegalStateException("Falta -Dpagoflex.it.redis.port (Redis de pruebas)");
        }
        String host = System.getProperty("pagoflex.it.redis.host", "127.0.0.1");
        factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, Integer.parseInt(port)));
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        factory.destroy();
    }

    @BeforeEach
    void setup() {
        // Redis exclusivo de estas pruebas: se parte siempre de una base vacia
        redis.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
        transactions = new RedisTransactionExecutor(redis);
        cache = new RedisExpiringCache(redis);
        sessions = new RedisAuthSessionService(redis, transactions, jwt, users, Duration.ofDays(7));
        codes = new VerificationCodeServiceImpl(transactions, cache);
        ReflectionTestUtils.setField(codes, "codeExpirationMinutes", 10L);
        ReflectionTestUtils.setField(codes, "resetExpirationMinutes", 10L);
        validator = new RedisAccessTokenValidator(redis);
        decoder = NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
        user = Users.builder().id(UUID.randomUUID()).email("it-" + UUID.randomUUID() + "@example.com")
                .role(Role.USUARIO_FINAL).build();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
    }

    // ---------- sesiones ----------
    @Test
    void issueStoresSessionWithExpirationAndAccessTokenIsAccepted() {
        AuthResponse auth = sessions.issue(user);

        assertThat(redis.keys("auth:session:*")).hasSize(1);
        assertThat(redis.keys("auth:access:*")).hasSize(1);
        assertThat(redis.opsForSet().members("auth:user:" + user.getId())).hasSize(1);
        Long accessTtl = redis.getExpire(redis.keys("auth:access:*").iterator().next());
        Long sessionTtl = redis.getExpire(redis.keys("auth:session:*").iterator().next());
        assertThat(accessTtl).isBetween(1L, 900L);
        assertThat(sessionTtl).isBetween(1L, Duration.ofDays(7).toSeconds());
        assertThat(validator.validate(decoder.decode(auth.tokenAcceso())).hasErrors()).isFalse();
    }

    @Test
    void refreshRotatesCredentialsAndRejectsTheSpentRefresh() {
        AuthResponse first = sessions.issue(user);
        AuthResponse second = sessions.refresh(first.tokenRenovacion());

        assertThat(second.tokenRenovacion()).isNotEqualTo(first.tokenRenovacion());
        assertThat(validator.validate(decoder.decode(first.tokenAcceso())).hasErrors()).isTrue();
        assertThat(validator.validate(decoder.decode(second.tokenAcceso())).hasErrors()).isFalse();
        assertThatThrownBy(() -> sessions.refresh(first.tokenRenovacion())).isInstanceOf(InvalidSession.class);
    }

    @Test
    void reusingASpentRefreshRevokesTheWholeSession() {
        AuthResponse first = sessions.issue(user);
        AuthResponse second = sessions.refresh(first.tokenRenovacion());

        // reutilizar el refresh ya gastado revoca la sesion, incluido el refresh vigente
        assertThatThrownBy(() -> sessions.refresh(first.tokenRenovacion())).isInstanceOf(InvalidSession.class);
        assertThatThrownBy(() -> sessions.refresh(second.tokenRenovacion())).isInstanceOf(InvalidSession.class);
        assertThat(validator.validate(decoder.decode(second.tokenAcceso())).hasErrors()).isTrue();
    }

    @Test
    void concurrentRefreshOfTheSameTokenSucceedsOnlyOnce() throws Exception {
        AuthResponse auth = sessions.issue(user);

        List<Object> outcomes = runConcurrently(6, () -> {
            try {
                return sessions.refresh(auth.tokenRenovacion());
            } catch (InvalidSession ex) {
                return ex;
            }
        });

        assertThat(outcomes.stream().filter(AuthResponse.class::isInstance)).hasSize(1);
    }

    @Test
    void revokeAllInvalidatesEverySessionOfTheUser() {
        AuthResponse a = sessions.issue(user);
        AuthResponse b = sessions.issue(user);

        sessions.revokeAll(user.getId());

        assertThat(validator.validate(decoder.decode(a.tokenAcceso())).hasErrors()).isTrue();
        assertThat(validator.validate(decoder.decode(b.tokenAcceso())).hasErrors()).isTrue();
        assertThat(redis.keys("auth:session:*")).isEmpty();
        assertThat(redis.keys("auth:refresh:*")).isEmpty();
        assertThatThrownBy(() -> sessions.refresh(a.tokenRenovacion())).isInstanceOf(InvalidSession.class);
    }

    @Test
    void replaceCurrentLeavesOnlyTheNewSession() {
        AuthResponse old = sessions.issue(user);
        AuthResponse fresh = sessions.replaceCurrent(user);

        assertThat(validator.validate(decoder.decode(old.tokenAcceso())).hasErrors()).isTrue();
        assertThat(validator.validate(decoder.decode(fresh.tokenAcceso())).hasErrors()).isFalse();
        assertThat(redis.keys("auth:session:*")).hasSize(1);
    }

    @Test
    void validatorRejectsAnExpiredSessionKey() {
        AuthResponse auth = sessions.issue(user);
        Jwt token = decoder.decode(auth.tokenAcceso());
        redis.delete("auth:access:" + token.getId());

        assertThat(validator.validate(token).hasErrors()).isTrue();
    }

    // ---------- expiracion real de claves ----------
    @Test
    void sessionDisappearsWhenItsTtlExpires() throws Exception {
        RedisAuthSessionService shortLived = new RedisAuthSessionService(redis, transactions, jwt, users,
                Duration.ofSeconds(1));
        AuthResponse auth = shortLived.issue(user);
        assertThat(redis.keys("auth:session:*")).hasSize(1);

        Thread.sleep(1500);

        assertThat(redis.keys("auth:session:*")).isEmpty();
        assertThat(redis.keys("auth:refresh:*")).isEmpty();
        assertThatThrownBy(() -> shortLived.refresh(auth.tokenRenovacion())).isInstanceOf(InvalidSession.class);
    }

    @Test
    void recoveryCodeIsStoredWithItsConfiguredExpiration() {
        codes.generateCode(user.getEmail(), TipoCodigo.RECOVERY);

        Long ttl = redis.getExpire(redis.keys("pagoflex:auth:code:*").iterator().next());

        assertThat(ttl).isBetween(1L, 600L);
    }

    // ---------- recuperacion de contrasena ----------
    @Test
    void recoveryCodeFlowIssuesAOneTimeResetToken() {
        String code = codes.generateCode(user.getEmail(), TipoCodigo.RECOVERY);
        // en redis solo existe el hash, no el codigo
        assertThat(redis.keys("pagoflex:auth:code:*")).hasSize(1);
        assertThat(redis.opsForValue().get(redis.keys("pagoflex:auth:code:*").iterator().next()))
                .isNotEqualTo(code);

        String reset = codes.verifyCodeAndIssueResetToken(user.getEmail(), TipoCodigo.RECOVERY, code);

        assertThat(redis.keys("pagoflex:auth:code:*")).isEmpty();
        assertThat(codes.consumeResetToken(reset)).isEqualTo(user.getEmail());
        assertThatThrownBy(() -> codes.consumeResetToken(reset)).isInstanceOf(InvalidResetToken.class);
        assertThatThrownBy(() -> codes.verifyCodeAndIssueResetToken(user.getEmail(), TipoCodigo.RECOVERY, code))
                .isInstanceOf(InvalidRecoveryCode.class);
    }

    @Test
    void fiveWrongAttemptsLockTheCodeEvenIfTheCorrectOneArrivesLater() {
        String code = codes.generateCode(user.getEmail(), TipoCodigo.RECOVERY);
        String wrong = code.startsWith("A") ? "BBBBBB" : "AAAAAA";

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> codes.verifyCodeAndIssueResetToken(user.getEmail(), TipoCodigo.RECOVERY, wrong))
                    .isInstanceOf(InvalidRecoveryCode.class);
        }

        assertThatThrownBy(() -> codes.verifyCodeAndIssueResetToken(user.getEmail(), TipoCodigo.RECOVERY, code))
                .isInstanceOf(InvalidRecoveryCode.class);
    }

    @Test
    void generatingANewCodeInvalidatesThePreviousResetToken() {
        String first = codes.generateCode(user.getEmail(), TipoCodigo.RECOVERY);
        String reset = codes.verifyCodeAndIssueResetToken(user.getEmail(), TipoCodigo.RECOVERY, first);

        codes.generateCode(user.getEmail(), TipoCodigo.RECOVERY);

        assertThatThrownBy(() -> codes.consumeResetToken(reset)).isInstanceOf(InvalidResetToken.class);
    }

    @Test
    void sendThrottleAllowsOnlyOneSendPerCooldownWindow() {
        RecoverySendThrottle throttle = new RecoverySendThrottle(redis);

        assertThat(throttle.allow(user.getEmail())).isTrue();
        assertThat(throttle.allow(user.getEmail())).isFalse();
        assertThat(throttle.allow("otro-" + user.getEmail())).isTrue();
        Long ttl = redis.getExpire(redis.keys("pagoflex:auth:send-cooldown:*").iterator().next());
        assertThat(ttl).isBetween(1L, 60L);
    }

    // --- helper: ejecuta la misma tarea desde varios hilos a la vez ---
    private static List<Object> runConcurrently(int threads, Callable<Object> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) results.add(future.get());
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
