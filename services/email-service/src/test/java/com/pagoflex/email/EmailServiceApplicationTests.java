package com.pagoflex.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmailServiceApplicationTests {
    @Autowired
    ApplicationContext context;

    @Test
    void contextLoads() {
    }

    // la autenticacion es solo la clave interna: no debe existir el usuario con contrasena generada
    @Test
    void doesNotCreateDefaultUserDetailsService() {
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
    }
}
