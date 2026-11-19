package com.pagoflex.cobranza.features.compromiso;

import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.core.exceptions.UsuariosNoDisponible;
import com.pagoflex.cobranza.features.compromiso.data.CargaDemoAcceso;
import com.pagoflex.cobranza.features.compromiso.repository.AnulacionRepository;
import com.pagoflex.cobranza.features.compromiso.repository.ComprobanteRepository;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.repository.PagoRepository;
import com.pagoflex.cobranza.features.compromiso.service.CobranzaEjecutivoService;
import com.pagoflex.cobranza.features.empresa.repository.EjecutivoEmpresaRepository;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.cobranza.features.empresa.service.EjecutivoService;
import com.pagoflex.cobranza.support.RelojFijo;
import com.pagoflex.contracts.usuarios.CrearPersonaInterna;
import com.pagoflex.contracts.usuarios.PersonaInterna;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(RelojFijo.class)
class CargaDemoTest {
    @Autowired
    private EmpresaClienteRepository empresaRepo;
    @Autowired
    private CompromisoRepository compromisoRepo;
    @Autowired
    private AnulacionRepository anulacionRepo;
    @Autowired
    private PagoRepository pagoRepo;
    @Autowired
    private ComprobanteRepository comprobanteRepo;
    @Autowired
    private EjecutivoEmpresaRepository ejecutivoRepo;
    @Autowired
    private EjecutivoService ejecutivoService;
    @Autowired
    private CobranzaEjecutivoService cobranza;
    @Autowired
    private TransactionTemplate tx;

    private final UsuariosServiceClient usuarios = mock(UsuariosServiceClient.class);
    private final Map<String, UUID> cuentas = new ConcurrentHashMap<>();

    @BeforeEach
    void preparar() {
        comprobanteRepo.deleteAll();
        pagoRepo.deleteAll();
        anulacionRepo.deleteAll();
        compromisoRepo.deleteAll();
        ejecutivoRepo.deleteAll();

        // usuarios responde con cuentas y crea las personas que faltan
        when(usuarios.porCorreo(anyString())).thenAnswer(i -> Optional.of(new PersonaInterna(UUID.randomUUID(),
                "1-9", "N", "A", i.getArgument(0), null, cuentas.computeIfAbsent(i.getArgument(0), k -> UUID.randomUUID()))));
        when(usuarios.porRut(anyString())).thenReturn(Optional.empty());
        when(usuarios.crear(any(CrearPersonaInterna.class))).thenAnswer(i -> {
            CrearPersonaInterna r = i.getArgument(0);
            return new PersonaInterna(UUID.randomUUID(), r.rut(), r.nombres(), r.apellidos(), r.correo(), r.telefono(), null);
        });
    }

    @Test
    void cargaElAnexoConSusPagosYUnaTasaDe881() {
        var carga = CargaDemoAcceso.crear(empresaRepo, compromisoRepo, anulacionRepo, pagoRepo, ejecutivoService, usuarios, tx);

        carga.cargar();
        carga.cargar();

        // 12 del anexo + 119 de septiembre, sin repetir la segunda vez
        assertThat(compromisoRepo.count()).isEqualTo(131);
        assertThat(pagoRepo.count()).isEqualTo(3 + 104);
        assertThat(anulacionRepo.count()).isEqualTo(3);
        var resumen = cobranza.resumen(cuentas.get("psaavedra@altosmaipo.ejemplo"), YearMonth.of(2026, 9));
        assertThat(resumen.totalCompromisos()).isEqualTo(120);
        assertThat(resumen.pagados()).isEqualTo(104);
        assertThat(resumen.tasaCobranza()).isEqualTo(88.1);
        assertThat(resumen.recaudado()).isEqualTo(32_476_000);
    }

    @Test
    void reintentaMientrasUsuariosNoResponde() {
        when(usuarios.porCorreo(anyString()))
                .thenThrow(new UsuariosNoDisponible())
                .thenAnswer(i -> Optional.of(new PersonaInterna(UUID.randomUUID(), "1-9", "N", "A",
                        i.getArgument(0), null, UUID.randomUUID())));
        var carga = CargaDemoAcceso.crear(empresaRepo, compromisoRepo, anulacionRepo, pagoRepo, ejecutivoService, usuarios, tx);

        carga.cargarConReintentos();

        assertThat(compromisoRepo.count()).isEqualTo(131);
    }
}
