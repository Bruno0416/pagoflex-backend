package com.pagoflex.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;


public final class RedisAccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private final StringRedisTemplate redis;

    public RedisAccessTokenValidator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    // ---------- comprobacion de sesion ----------
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            // 1. exigir identificadores de token y sesion validos
            String sid = jwt.getClaimAsString("sid");
            String jti = jwt.getId();
            UUID.fromString(sid);
            UUID.fromString(jti);
            UUID.fromString(jwt.getSubject());

            // 2. comprobar que el acceso siga vinculado a la sesion vigente
            String linked = redis.opsForValue().get("auth:access:" + jti);
            if (!sid.equals(linked)) return invalid();
            String session = redis.opsForValue().get("auth:session:" + sid);
            if (session == null) return invalid();
            String[] fields = session.split("\\|", -1);
            if (fields.length != 3 || !jwt.getSubject().equals(fields[0]) || !jti.equals(fields[1])) {
                return invalid();
            }
            return OAuth2TokenValidatorResult.success();
        } catch (RuntimeException ex) {
            // 3. denegar el acceso tambien cuando redis no este disponible
            return invalid();
        }
    }

    private static OAuth2TokenValidatorResult invalid() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sesion revocada o no disponible", null));
    }
}
