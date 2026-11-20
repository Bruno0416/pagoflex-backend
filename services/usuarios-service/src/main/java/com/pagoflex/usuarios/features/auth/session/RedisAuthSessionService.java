package com.pagoflex.usuarios.features.auth.session;

import com.pagoflex.security.CurrentUser;
import com.pagoflex.usuarios.core.cache.RedisTransactionExecutor;
import com.pagoflex.usuarios.core.security.JwtUtil;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidSession;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class RedisAuthSessionService implements AuthSessionService {
    private final StringRedisTemplate redis;
    private final RedisTransactionExecutor transactions;
    private final JwtUtil jwtUtil;
    private final UserRepository users;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public RedisAuthSessionService(StringRedisTemplate redis, RedisTransactionExecutor transactions,
                                   JwtUtil jwtUtil, UserRepository users,
                                   @Value("${pagoflex.security.jwt.refresh-expiration:7d}") Duration refreshTtl) {
        if (refreshTtl.isNegative() || refreshTtl.isZero()) {
            throw new IllegalArgumentException("La duracion de refresh debe ser positiva");
        }
        this.redis = redis;
        this.transactions = transactions;
        this.jwtUtil = jwtUtil;
        this.users = users;
        this.refreshTtl = refreshTtl;
    }

    // ---------- inicio de sesion ----------
    @Override
    public AuthResponse issue(Users user) {
        UUID sid = UUID.randomUUID();
        UUID jti = UUID.randomUUID();
        String refresh = newRefresh();
        String hash = hash(refresh);
        String token = jwtUtil.generateToken(user, sid, jti);

        // 1. persistir la sesion y su par de tokens en una sola transaccion redis
        Boolean success = transactions.execute(op -> {
            var result = transactions.commit(op, () -> {
                op.opsForValue().set(sessionKey(sid), entry(user.getId(), jti, hash), refreshTtl);
                op.opsForValue().set(accessKey(jti), sid.toString(), Duration.ofSeconds(jwtUtil.expiresInSeconds()));
                op.opsForValue().set(refreshKey(hash), sid.toString(), refreshTtl);
                op.opsForSet().add(userKey(user.getId()), sid.toString());
                op.expire(userKey(user.getId()), refreshTtl);
            });
            return result != null;
        });
        if (!Boolean.TRUE.equals(success)) throw new InvalidSession();
        return response(token, refresh);
    }

    // ---------- renovacion de tokens ----------
    @Override
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) throw new InvalidSession();
        String oldHash = hash(refreshToken);
        String sidText = redis.opsForValue().get(refreshKey(oldHash));
        if (sidText == null) {
            // 1. detectar reutilizacion del refresh anterior
            String spentSid = redis.opsForValue().get(spentKey(oldHash));
            if (spentSid != null) revokeSession(UUID.fromString(spentSid));
            throw new InvalidSession();
        }
        UUID sid = parseUuid(sidText);
        String old = redis.opsForValue().get(sessionKey(sid));
        if (old == null) throw new InvalidSession();
        String[] fields = parseEntry(old);
        if (!oldHash.equals(fields[2])) throw new InvalidSession();
        UUID userId = parseUuid(fields[0]);
        Users user = users.findById(userId).orElseThrow(InvalidSession::new);
        UUID oldJti = parseUuid(fields[1]);
        UUID newJti = UUID.randomUUID();
        String newRefresh = newRefresh();
        String newHash = hash(newRefresh);
        String newAccess = jwtUtil.generateToken(user, sid, newJti);

        // 2. sustituir ambas credenciales de manera atomica
        Boolean success = transactions.execute(op -> {
            op.watch(List.of(sessionKey(sid), refreshKey(oldHash)));
            try {
                if (!old.equals(op.opsForValue().get(sessionKey(sid)))
                        || !sidText.equals(op.opsForValue().get(refreshKey(oldHash)))) return false;
                var result = transactions.commit(op, () -> {
                    op.delete(accessKey(oldJti));
                    op.delete(refreshKey(oldHash));
                    op.opsForValue().set(spentKey(oldHash), sid.toString(), refreshTtl);
                    op.opsForValue().set(sessionKey(sid), entry(userId, newJti, newHash), refreshTtl);
                    op.opsForValue().set(accessKey(newJti), sid.toString(), Duration.ofSeconds(jwtUtil.expiresInSeconds()));
                    op.opsForValue().set(refreshKey(newHash), sid.toString(), refreshTtl);
                    op.expire(userKey(userId), refreshTtl);
                });
                return result != null;
            } finally {
                op.unwatch();
            }
        });
        if (!Boolean.TRUE.equals(success)) throw new InvalidSession();
        return response(newAccess, newRefresh);
    }

    // ---------- revocacion de sesiones ----------
    @Override
    public void revokeCurrent() {
        UUID sid = parseUuid(CurrentUser.jwt().getClaimAsString("sid"));
        String stored = redis.opsForValue().get(sessionKey(sid));
        if (stored == null || !CurrentUser.id().toString().equals(parseEntry(stored)[0])) {
            throw new InvalidSession();
        }
        revokeSession(sid);
    }

    @Override
    public void revokeAll(UUID userId) {
        var sessions = redis.opsForSet().members(userKey(userId));
        if (sessions == null) return;
        // 1. invalidar todas las sesiones y accesos vigentes del usuario
        for (String sid : sessions) revokeSession(parseUuid(sid));
        redis.delete(userKey(userId));
    }

    @Override
    public AuthResponse replaceCurrent(Users user) {
        // 1. invalidar el resto de sesiones antes de emitir credenciales nuevas
        revokeAll(user.getId());
        return issue(user);
    }

    // --- helpers ---
    private void revokeSession(UUID sid) {
        String stored = redis.opsForValue().get(sessionKey(sid));
        if (stored == null) return;
        String[] fields = parseEntry(stored);
        UUID uid = parseUuid(fields[0]);
        UUID jti = parseUuid(fields[1]);
        String refreshHash = fields[2];
        Boolean success = transactions.execute(op -> {
            op.watch(sessionKey(sid));
            try {
                if (!stored.equals(op.opsForValue().get(sessionKey(sid)))) return false;
                var result = transactions.commit(op, () -> {
                    op.delete(accessKey(jti));
                    op.delete(refreshKey(refreshHash));
                    op.delete(sessionKey(sid));
                    op.opsForSet().remove(userKey(uid), sid.toString());
                });
                return result != null;
            } finally { op.unwatch(); }
        });
        if (!Boolean.TRUE.equals(success)) throw new InvalidSession();
    }

    private String newRefresh() {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    private static String hash(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static UUID parseUuid(String value) {
        try { return UUID.fromString(value); }
        catch (RuntimeException ex) { throw new InvalidSession(); }
    }

    private static String[] parseEntry(String value) {
        String[] fields = value.split("\\|", -1);
        if (fields.length != 3) throw new InvalidSession();
        return fields;
    }

    private static String entry(UUID uid, UUID jti, String refreshHash) {
        return uid + "|" + jti + "|" + refreshHash;
    }

    private AuthResponse response(String access, String refresh) {
        return new AuthResponse(access, refresh, "Bearer", jwtUtil.expiresInSeconds());
    }

    private static String sessionKey(UUID sid) { return "auth:session:" + sid; }
    private static String accessKey(UUID jti) { return "auth:access:" + jti; }
    private static String refreshKey(String digest) { return "auth:refresh:" + digest; }
    private static String spentKey(String digest) { return "auth:spent:" + digest; }
    private static String userKey(UUID uid) { return "auth:user:" + uid; }
}
