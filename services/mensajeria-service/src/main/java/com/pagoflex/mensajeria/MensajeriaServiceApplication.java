package com.pagoflex.mensajeria;

import com.pagoflex.web.error.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class MensajeriaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MensajeriaServiceApplication.class, args);
    }
}
