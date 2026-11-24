package com.pagoflex.cobranza.features.codigo.data;

import com.pagoflex.cobranza.features.codigo.service.CodigoRegistroService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@ConditionalOnProperty(name = "pagoflex.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CargaCodigosDemo implements ApplicationRunner {
    private final CodigoRegistroService codigoService;

    @Override
    public void run(ApplicationArguments args) {
        // 1. preparar el codigo de agentes despues de la carga de empresas
        codigoService.aprovisionarDemo("INTERNA");
        // 2. preparar un codigo fijo por empresa cliente
        for (int i = 1; i <= 8; i++) {
            codigoService.aprovisionarDemo("EC-%02d".formatted(i));
        }
    }
}
