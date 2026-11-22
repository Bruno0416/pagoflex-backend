package com.pagoflex.cobranza.features.compromiso.data;

import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.features.compromiso.repository.AnulacionRepository;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.repository.PagoRepository;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import org.springframework.transaction.support.TransactionTemplate;

/// Abre la carga demo en las pruebas sin esperas entre reintentos
public final class CargaDemoAcceso {
    private CargaDemoAcceso() {
    }

    public static Prueba crear(EmpresaClienteRepository empresaRepo, CompromisoRepository compromisoRepo,
                               AnulacionRepository anulacionRepo, PagoRepository pagoRepo,
                               UsuariosServiceClient usuarios, TransactionTemplate tx) {
        return new Prueba(new CargaDemo(empresaRepo, compromisoRepo, anulacionRepo, pagoRepo,
                usuarios, tx, 1));
    }

    public record Prueba(CargaDemo carga) {
        public void cargar() {
            carga.cargar();
        }

        public void cargarConReintentos() {
            carga.cargarConReintentos();
        }
    }
}
