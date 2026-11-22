package com.pagoflex.email;

import com.pagoflex.web.error.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;

// la autenticacion es la clave interna de SecurityConfig; no se necesita el usuario generado por defecto
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
public class EmailServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmailServiceApplication.class, args);
    }
}
