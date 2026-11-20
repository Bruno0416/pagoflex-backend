package com.pagoflex.reconocimientos;

import com.pagoflex.web.error.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class ReconocimientosServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReconocimientosServiceApplication.class, args);
    }
}
