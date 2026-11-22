package com.pagoflex.usuarios.features.auth.session;

import com.pagoflex.usuarios.core.cache.RedisTransactionExecutor;
import com.pagoflex.usuarios.core.security.JwtUtil;
import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidSession;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisAuthSessionServiceTest {
    @Mock StringRedisTemplate redis;
    @Mock RedisTransactionExecutor transactions;
    @Mock JwtUtil jwt;
    @Mock UserRepository users;
    @Mock RedisOperations<String, String> operations;
    @Mock ValueOperations<String, String> values;
    @Mock SetOperations<String, String> sets;

    private AuthSessionService service;
    private final UUID uid = UUID.randomUUID();
    private final Users user = Users.builder().id(uid).email("usuario@example.com")
            .role(Role.USUARIO_FINAL).build();

    @BeforeEach
    void setup() {
        service = new RedisAuthSessionService(redis, transactions, jwt, users, Duration.ofDays(7));
        lenient().when(redis.opsForValue()).thenReturn(values);
        lenient().when(redis.opsForSet()).thenReturn(sets);
        lenient().when(operations.opsForValue()).thenReturn(values);
        lenient().when(operations.opsForSet()).thenReturn(sets);
        lenient().when(jwt.expiresInSeconds()).thenReturn(900L);
        lenient().when(transactions.execute(any())).thenAnswer(inv -> {
            Function<RedisOperations<String, String>, ?> callback = inv.getArgument(0);
            return callback.apply(operations);
        });
        lenient().when(transactions.commit(eq(operations), any())).thenAnswer(inv -> {
            Runnable changes = inv.getArgument(1);
            changes.run();
            return List.of(true);
        });
    }

    // ---------- emision y rotacion ----------
    @Test
    void issuesSessionAndStoresBothTokenReferences() {
        when(jwt.generateToken(eq(user), any(UUID.class), any(UUID.class))).thenReturn("signed-access");
        var response = service.issue(user);
        assertThat(response.tokenAcceso()).isEqualTo("signed-access");
        assertThat(response.tokenRenovacion()).isNotBlank();
        assertThat(response.rol()).isEqualTo(Role.USUARIO_FINAL);
        verify(operations, atLeastOnce()).opsForValue();
        verify(values, times(3)).set(anyString(), anyString(), any(Duration.class));
        verify(sets).add(eq("auth:user:" + uid), anyString());
    }

    @Test
    void refreshConsumesOldPairAndCreatesNewPair() throws Exception {
        UUID sid = UUID.randomUUID();
        UUID oldJti = UUID.randomUUID();
        String oldRefresh = "refresh-vigente";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(oldRefresh.getBytes(StandardCharsets.UTF_8)));
        String oldEntry = uid + "|" + oldJti + "|" + hash;
        when(values.get("auth:refresh:" + hash)).thenReturn(sid.toString());
        when(values.get("auth:session:" + sid)).thenReturn(oldEntry);
        when(users.findById(uid)).thenReturn(Optional.of(user));
        when(jwt.generateToken(eq(user), eq(sid), any(UUID.class))).thenReturn("new-access");

        var result = service.refresh(oldRefresh);
        assertThat(result.tokenAcceso()).isEqualTo("new-access");
        assertThat(result.tokenRenovacion()).isNotEqualTo(oldRefresh);
        verify(operations).watch(List.of("auth:session:" + sid, "auth:refresh:" + hash));
        verify(operations).delete("auth:access:" + oldJti);
        verify(operations).delete("auth:refresh:" + hash);
        verify(values).set(eq("auth:spent:" + hash), eq(sid.toString()), eq(Duration.ofDays(7)));
    }

    @Test
    void rejectsExpiredRefreshWithoutCreatingTokens() {
        assertThatThrownBy(() -> service.refresh("expired"))
                .isInstanceOf(InvalidSession.class);
        verify(jwt, never()).generateToken(any(), any(), any());
    }

    @Test
    void rejectsBlankRefreshWithoutRedisReads() {
        assertThatThrownBy(() -> service.refresh(" ")).isInstanceOf(InvalidSession.class);
        verifyNoInteractions(redis);
    }

    @Test
    void rejectsInvalidSessionReference() {
        when(values.get(startsWith("auth:refresh:"))).thenReturn("not-a-uuid");
        assertThatThrownBy(() -> service.refresh("refresh"))
                .isInstanceOf(InvalidSession.class);
    }

    @Test
    void revokeAllDeletesTokensOfEverySession() {
        UUID sid = UUID.randomUUID();
        UUID jti = UUID.randomUUID();
        when(sets.members("auth:user:" + uid)).thenReturn(Set.of(sid.toString()));
        when(values.get("auth:session:" + sid)).thenReturn(uid + "|" + jti + "|hash");

        service.revokeAll(uid);
        verify(operations).delete("auth:access:" + jti);
        verify(operations).delete("auth:refresh:hash");
        verify(operations).delete("auth:session:" + sid);
        verify(redis).delete("auth:user:" + uid);
    }

    @Test
    void revokeAllHandlesMissingIndex() {
        when(sets.members("auth:user:" + uid)).thenReturn(null);
        service.revokeAll(uid);
        verify(redis, never()).delete(anyString());
    }

    @Test
    void revokeAllCleansUpEmptyIndex() {
        when(sets.members("auth:user:" + uid)).thenReturn(Set.of());
        service.revokeAll(uid);
        verify(redis).delete("auth:user:" + uid);
    }

    @Test
    void refreshFailsWhenConcurrentRequestWinsWatch() throws Exception {
        UUID sid = UUID.randomUUID();
        UUID oldJti = UUID.randomUUID();
        String refresh = "refresh-antiguo";
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(refresh.getBytes(StandardCharsets.UTF_8)));
        when(values.get("auth:refresh:" + digest)).thenReturn(sid.toString());
        when(values.get("auth:session:" + sid)).thenReturn(uid + "|" + oldJti + "|" + digest);
        when(users.findById(uid)).thenReturn(Optional.of(user));
        when(transactions.commit(eq(operations), any())).thenReturn(null);
        assertThatThrownBy(() -> service.refresh(refresh)).isInstanceOf(InvalidSession.class);
    }

    @Test
    void reusedRefreshRevokesItsSession() throws Exception {
        UUID sid = UUID.randomUUID();
        UUID oldJti = UUID.randomUUID();
        String refresh = "refresh-consumido";
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(refresh.getBytes(StandardCharsets.UTF_8)));
        // 1. simular que el refresh anterior ya fue consumido
        when(values.get("auth:refresh:" + digest)).thenReturn(null);
        when(values.get("auth:spent:" + digest)).thenReturn(sid.toString());
        when(values.get("auth:session:" + sid)).thenReturn(uid + "|" + oldJti + "|new-hash");
        assertThatThrownBy(() -> service.refresh(refresh)).isInstanceOf(InvalidSession.class);
        verify(operations).delete("auth:session:" + sid);
        verify(operations).delete("auth:access:" + oldJti);
    }

    @Test
    void replaceCurrentRevokesAndCreatesFreshSession() {
        when(sets.members("auth:user:" + uid)).thenReturn(Set.of());
        when(jwt.generateToken(eq(user), any(UUID.class), any(UUID.class))).thenReturn("nuevo-access");
        var tokens = service.replaceCurrent(user);
        assertThat(tokens.tokenAcceso()).isEqualTo("nuevo-access");
        verify(redis).delete("auth:user:" + uid);
        verify(sets).add(eq("auth:user:" + uid), anyString());
    }


    @Test
    void rejectsInternalRoleWithoutCompany() {
        Users agente = Users.builder().id(uid)
                .email("agente@example.com").role(Role.AGENTE).build();

        assertThatThrownBy(() -> service.issue(agente))
                .isInstanceOf(InvalidSession.class);
        verify(jwt, never()).generateToken(any(), any(), any());
    }

    @Test
    void issuesValidInternalRoleInAuthResponse() {
        Users agente = Users.builder().id(uid)
                .email("agente@example.com").role(Role.AGENTE)
                .persona(com.pagoflex.usuarios.features.profile.entity.Persona
                        .builder().empresaId(UUID.randomUUID()).build())
                .build();
        when(jwt.generateToken(eq(agente), any(UUID.class), any(UUID.class)))
                .thenReturn("signed-access");

        var response = service.issue(agente);
        assertThat(response.rol()).isEqualTo(Role.AGENTE);
        assertThat(response.tokenAcceso()).isEqualTo("signed-access");
    }

}
