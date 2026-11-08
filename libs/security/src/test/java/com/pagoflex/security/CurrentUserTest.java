package com.pagoflex.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {
    private static final UUID USER_ID = UUID.fromString("12a7be82-ecaa-490d-a69b-5d6b1edacbb7");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exposesIdentityAndRolesFromAuthenticatedJwt() {
        Jwt jwt = jwt(USER_ID.toString(), List.of("USUARIO_FINAL"));
        SecurityContextHolder.getContext().setAuthentication(authenticated(jwt));

        assertThat(CurrentUser.id()).isEqualTo(USER_ID);
        assertThat(CurrentUser.get().email()).isEqualTo("usuario@example.com");
        assertThat(CurrentUser.get().roles()).containsExactly("USUARIO_FINAL");
        assertThat(CurrentUser.hasRole("USUARIO_FINAL")).isTrue();
        assertThat(CurrentUser.hasRole("AGENTE")).isFalse();
        assertThat(CurrentUser.jwt().getClaimAsString("token_type")).isEqualTo("ACCESS");
    }

    @Test
    void rejectsMissingAuthentication() {
        assertThatThrownBy(CurrentUser::get)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void rejectsNonJwtAuthentication() {
        var anonymous = new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThatThrownBy(CurrentUser::get)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void rejectsNonUuidSubject() {
        SecurityContextHolder.getContext().setAuthentication(authenticated(jwt("not-a-uuid", List.of("USUARIO_FINAL"))));

        assertThatThrownBy(CurrentUser::get)
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    void rejectsMissingRoles() {
        SecurityContextHolder.getContext().setAuthentication(authenticated(jwt(USER_ID.toString(), List.of())));

        assertThatThrownBy(CurrentUser::get)
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    void rejectsUnauthenticatedJwt() {
        Jwt jwt = jwt(USER_ID.toString(), List.of("USUARIO_FINAL"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThatThrownBy(CurrentUser::get)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    private JwtAuthenticationToken authenticated(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USUARIO_FINAL")));
    }

    private Jwt jwt(String subject, List<String> roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject)
                .claim("token_type", "ACCESS")
                .claim("userId", subject)
                .claim("email", "usuario@example.com")
                .claim("roles", roles)
                .build();
    }
}
