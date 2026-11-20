package com.pagoflex.soporte.features.reporte.data;

import com.pagoflex.contracts.cobranza.CompromisoInterno;
import com.pagoflex.soporte.core.client.PersonaInterna;
import com.pagoflex.soporte.core.client.CobranzaServiceClient;
import com.pagoflex.soporte.core.client.UsuariosServiceClient;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.entity.MotivoReporte;
import com.pagoflex.soporte.features.reporte.entity.Reporte;
import com.pagoflex.soporte.features.reporte.repository.ReporteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/// Reportes del anexo 9, los mismos que trae la app; los cobros coinciden con la carga demo de cobranza.
/// Solo con pagoflex.demo.enabled=true. Usuarios y cobranza pueden partir después, por eso se reintenta
@Slf4j
@Component
@ConditionalOnProperty(name = "pagoflex.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CargaDemo {
    private final ReporteRepository repository;
    private final CobranzaServiceClient cobranza;
    private final UsuariosServiceClient usuarios;
    private final TransactionTemplate transaccion;

    @Value("${pagoflex.demo.reintento-ms:5000}")
    private long esperaMs;

    @Value("${pagoflex.demo.intentos:36}")
    private int intentos;

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        // 1. cargar en segundo plano para no frenar el arranque
        Thread hilo = new Thread(this::reintentar, "carga-demo-soporte");
        hilo.setDaemon(true);
        hilo.start();
    }

    // un intento completo; true si ya quedaron cargados
    public synchronized boolean intentarCarga() {
        // 1. no repetir la carga
        if (repository.count() > 0) return true;
        try {
            // 2. uuid de las cuentas de ejemplo y datos actuales de los cobros
            PersonaInterna hector = usuarios.porCorreo("hpizarro@correo.ejemplo");
            PersonaInterna matias = usuarios.porCorreo("msoto@correo.ejemplo");
            PersonaInterna luis = usuarios.porCorreo("lmunoz@correo.ejemplo");
            PersonaInterna andres = usuarios.porCorreo("alagos@pagoflex.ejemplo");
            PersonaInterna daniela = usuarios.porCorreo("dortiz@pagoflex.ejemplo");
            CompromisoInterno cobro9 = cobranza.reportabilidad("CP-2026-0111", null);
            CompromisoInterno cobro11 = cobranza.reportabilidad("CP-2026-0109", null);
            CompromisoInterno cobro12 = cobranza.reportabilidad("CP-2026-0106", null);
            if (faltaAlguno(hector, matias, luis, andres, daniela) || faltaAlguno(cobro9, cobro11, cobro12)) {
                return false;
            }

            // 3. uno resuelto, uno en atención y uno abierto atrasado
            Reporte rep9 = base("REP-0009", cobro9, hector, MotivoReporte.MONTO_INCORRECTO,
                    "Repacté el crédito y me siguen cobrando la cuota antigua.", LocalDate.of(2026, 9, 18).atTime(9, 0));
            tomar(rep9, andres, LocalDate.of(2026, 9, 18).atTime(12, 0));
            atender(rep9, andres, LocalDate.of(2026, 9, 18).atTime(12, 30));
            rep9.setRespuesta("La empresa anuló la cuota por repactación (22-09-2026).");
            rep9.setFechaResolucion(LocalDate.of(2026, 9, 22).atTime(10, 0));
            rep9.registrarCambio(EstadoReporte.RESUELTO, andres.usuarioId(), andres.correo(), rep9.getFechaResolucion());

            Reporte rep11 = base("REP-0011", cobro11, matias, MotivoReporte.YA_PAGUE,
                    "Pagué por transferencia directa al instituto el 03-10.", LocalDate.of(2026, 10, 3).atTime(9, 0));
            tomar(rep11, daniela, LocalDate.of(2026, 10, 3).atTime(15, 0));
            atender(rep11, daniela, LocalDate.of(2026, 10, 3).atTime(15, 30));

            Reporte rep12 = base("REP-0012", cobro12, luis, MotivoReporte.MONTO_INCORRECTO,
                    "No sé por qué me cobran $318.500.", LocalDate.of(2026, 10, 5).atStartOfDay());

            transaccion.executeWithoutResult(s -> repository.saveAll(List.of(rep9, rep11, rep12)));
            log.info("carga demo de soporte lista: 3 reportes");
            return true;
        } catch (RuntimeException ex) {
            // usuarios o cobranza todavía no responden
            log.debug("carga demo de soporte en espera: {}", ex.getMessage());
            return false;
        }
    }

    // --- helpers privados ---
    private void reintentar() {
        for (int i = 0; i < intentos; i++) {
            if (intentarCarga()) return;
            try {
                Thread.sleep(esperaMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("no se pudo cargar la demo de soporte: usuarios o cobranza no respondieron");
    }

    private static boolean faltaAlguno(Object... valores) {
        for (Object v : valores) {
            if (v == null) return true;
        }
        return false;
    }

    private Reporte base(String numero, CompromisoInterno cobro, PersonaInterna usuario,
                         MotivoReporte motivo, String comentario, LocalDateTime creacion) {
        Reporte r = Reporte.builder()
                .numero(numero)
                .folio(cobro.folio())
                .motivo(motivo)
                .comentario(comentario)
                .fechaCreacion(creacion)
                .usuarioId(usuario.usuarioId())
                .usuarioCorreo(usuario.correo())
                .empresaId(cobro.empresaId())
                .empresa(cobro.empresa())
                .concepto(cobro.concepto())
                .numeroCuota(cobro.numeroCuota())
                .totalCuotas(cobro.totalCuotas())
                .estadoCompromiso(cobro.estado())
                .total(cobro.total())
                .fechaVencimiento(cobro.fechaVencimiento())
                .nombreUsuario(usuario.nombres() + " " + usuario.apellidos())
                .telefonoUsuario(usuario.telefono())
                .build();
        r.registrarCambio(EstadoReporte.ABIERTO, usuario.usuarioId(), usuario.correo(), creacion);
        return r;
    }

    private void tomar(Reporte r, PersonaInterna agente, LocalDateTime fecha) {
        r.setAgenteId(agente.usuarioId());
        r.setAgenteCorreo(agente.correo());
        r.setFechaToma(fecha);
        r.registrarCambio(EstadoReporte.ASIGNADO, agente.usuarioId(), agente.correo(), fecha);
    }

    private void atender(Reporte r, PersonaInterna agente, LocalDateTime fecha) {
        r.setFechaAtencion(fecha);
        r.registrarCambio(EstadoReporte.EN_ATENCION, agente.usuarioId(), agente.correo(), fecha);
    }
}
