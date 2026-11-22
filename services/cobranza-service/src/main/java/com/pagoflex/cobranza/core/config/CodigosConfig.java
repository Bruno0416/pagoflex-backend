package com.pagoflex.cobranza.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CodigosConfig {

    // BCrypt para guardar los códigos de registro; nunca se guardan en claro
    @Bean
    public PasswordEncoder codigoEncoder() {
        return new BCryptPasswordEncoder();
    }
}
