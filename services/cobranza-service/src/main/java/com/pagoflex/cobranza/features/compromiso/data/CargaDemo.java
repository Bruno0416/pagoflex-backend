package com.pagoflex.cobranza.features.compromiso.data;

import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.features.compromiso.entity.*;
import com.pagoflex.cobranza.features.compromiso.repository.AnulacionRepository;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.repository.PagoRepository;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.cobranza.features.empresa.service.EjecutivoService;
import com.pagoflex.contracts.usuarios.CrearPersonaInterna;
import com.pagoflex.contracts.usuarios.PersonaInterna;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/// Datos de ejemplo del anexo 9, los mismos que trae la app para probarla conectada.
/// Las personas y cuentas las pide a usuarios-service, por eso espera a que responda.
@Slf4j
@Component
@ConditionalOnProperty(name = "pagoflex.demo.enabled", havingValue = "true")
public class CargaDemo {
    private static final LocalDateTime CARGA = LocalDateTime.of(2026, 8, 30, 9, 0);
    private static final int INTENTOS = 36;

    private final EmpresaClienteRepository empresaRepo;
    private final CompromisoRepository compromisoRepo;
    private final AnulacionRepository anulacionRepo;
    private final PagoRepository pagoRepo;
    private final EjecutivoService ejecutivoService;
    private final UsuariosServiceClient usuarios;
    private final TransactionTemplate tx;
    private final long esperaMs;

    private final Map<String, PersonaInterna> personas = new HashMap<>();
    private final Map<String, UUID> ejecutivos = new HashMap<>();
    private int comprobantes;

    @Autowired
    public CargaDemo(EmpresaClienteRepository empresaRepo, CompromisoRepository compromisoRepo,
                     AnulacionRepository anulacionRepo, PagoRepository pagoRepo, EjecutivoService ejecutivoService,
                     UsuariosServiceClient usuarios, TransactionTemplate tx) {
        this(empresaRepo, compromisoRepo, anulacionRepo, pagoRepo, ejecutivoService, usuarios, tx, 5_000);
    }

    CargaDemo(EmpresaClienteRepository empresaRepo, CompromisoRepository compromisoRepo,
              AnulacionRepository anulacionRepo, PagoRepository pagoRepo, EjecutivoService ejecutivoService,
              UsuariosServiceClient usuarios, TransactionTemplate tx, long esperaMs) {
        this.empresaRepo = empresaRepo;
        this.compromisoRepo = compromisoRepo;
        this.anulacionRepo = anulacionRepo;
        this.pagoRepo = pagoRepo;
        this.ejecutivoService = ejecutivoService;
        this.usuarios = usuarios;
        this.tx = tx;
        this.esperaMs = esperaMs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        // en segundo plano para no frenar el arranque mientras usuarios despierta
        Thread hilo = new Thread(this::cargarConReintentos, "carga-demo-cobranza");
        hilo.setDaemon(true);
        hilo.start();
    }

    void cargarConReintentos() {
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                cargar();
                return;
            } catch (RuntimeException ex) {
                log.info("carga demo: usuarios-service aún no responde (intento {})", intento);
                dormir();
            }
        }
        log.warn("carga demo: no se pudo cargar, usuarios-service no respondió");
    }

    void cargar() {
        // 1. no repetir la carga
        if (compromisoRepo.count() > 0) return;

        // 2. pedir a usuarios las cuentas y personas antes de abrir la transacción
        resolverPersonas();

        // 3. guardar todo junto
        tx.executeWithoutResult(estado -> guardar());
        log.info("carga demo: compromisos de ejemplo listos");
    }

    // --- pasos ---
    private void resolverPersonas() {
        // 1. ejecutivos por el correo de su cuenta
        for (String correo : new String[]{"psaavedra@altosmaipo.ejemplo", "mfuentes@bosquenativo.ejemplo",
                "chenriquez@creditoandino.ejemplo", "fmorales@creditoandino.ejemplo", "gcarrasco@loshalcones.ejemplo",
                "snunez@itcordillera.ejemplo", "mleiva@plazaoriente.ejemplo"}) {
            ejecutivos.put(correo, usuarios.porCorreo(correo).map(PersonaInterna::usuarioId).orElseThrow());
        }

        // 2. usuarios finales del anexo
        persona("UF-01", "15834207-3", "Camila Andrea", "Rojas Peña", "crojas@correo.ejemplo", "+56 9 0000 0101");
        persona("UF-02", "12476318-5", "Luis Alberto", "Muñoz Tapia", "lmunoz@correo.ejemplo", "+56 9 0000 0102");
        persona("UF-03", "6893541-5", "Rosa Elena", "Contreras Díaz", "rcontreras@correo.ejemplo", "+56 9 0000 0103");
        persona("UF-04", "21045873-5", "Matías Ignacio", "Soto Vergara", "msoto@correo.ejemplo", "+56 9 0000 0104");
        persona("UF-05", "17562904-1", "Fernanda Paz", "Godoy Lagos", "fgodoy@correo.ejemplo", "+56 9 0000 0105");
        persona("UF-06", "10318265-4", "Héctor Manuel", "Pizarro Campos", "hpizarro@correo.ejemplo", "+56 9 0000 0106");
        persona("UF-07", "19287430-0", "Javiera Constanza", "Reyes Molina", "jreyes@correo.ejemplo", "+56 9 0000 0107");
        persona("UF-08", "18704396-4", "Benjamín Esteban", "Araya Fuentes", "baraya@correo.ejemplo", "+56 9 0000 0108");

        // 3. clientes sintéticos de septiembre de altos del maipo, sin cuenta
        for (int i = 0; i < 119; i++) {
            String cuerpo = String.valueOf(20_100_000 + (i + 1) * 137);
            String numero = "%03d".formatted(i + 1);
            persona("SEP-" + numero, cuerpo + "-" + digitoVerificador(cuerpo), "Cliente", "Demo " + numero,
                    "cliente" + numero + "@correo.ejemplo", "+56 9 0000 %04d".formatted(1000 + i));
        }
    }

    private void guardar() {
        // 1. cada ejecutivo con su empresa
        asignar("psaavedra@altosmaipo.ejemplo", "EC-01");
        asignar("mfuentes@bosquenativo.ejemplo", "EC-02");
        asignar("chenriquez@creditoandino.ejemplo", "EC-03");
        asignar("fmorales@creditoandino.ejemplo", "EC-03");
        asignar("gcarrasco@loshalcones.ejemplo", "EC-06");
        asignar("snunez@itcordillera.ejemplo", "EC-07");
        asignar("mleiva@plazaoriente.ejemplo", "EC-08");

        // 2. compromisos de octubre del anexo
        compromiso("CP-2026-0101", "UF-01", "EC-08", "Arriendo departamento octubre", 10, 12, 520_000, "2026-10-10", EstadoCompromiso.PENDIENTE, 0, "mleiva@plazaoriente.ejemplo", null);
        compromiso("CP-2026-0102", "UF-01", "EC-06", "Cuota social octubre", 10, 12, 15_000, "2026-10-05", EstadoCompromiso.VENCIDO, 2_000, "gcarrasco@loshalcones.ejemplo", null);
        compromiso("CP-2026-0103", "UF-01", "EC-05", "Aporte mensual octubre", null, null, 10_000, "2026-10-15", EstadoCompromiso.PENDIENTE, 0, null, null);
        compromiso("CP-2026-0104", "UF-01", "EC-03", "Crédito de consumo", 7, 24, 86_400, "2026-09-25", EstadoCompromiso.PAGADO, 0, "chenriquez@creditoandino.ejemplo", LocalDateTime.of(2026, 9, 24, 18, 30));
        compromiso("CP-2026-0105", "UF-01", "EC-03", "Crédito de consumo", 8, 24, 86_400, "2026-10-25", EstadoCompromiso.PENDIENTE, 0, "chenriquez@creditoandino.ejemplo", null);
        compromiso("CP-2026-0106", "UF-02", "EC-01", "Cuota pie departamento", 14, 36, 310_000, "2026-09-30", EstadoCompromiso.EN_REVISION, 8_500, "psaavedra@altosmaipo.ejemplo", null);
        compromiso("CP-2026-0107", "UF-02", "EC-01", "Cuota pie departamento", 15, 36, 310_000, "2026-10-30", EstadoCompromiso.PENDIENTE, 0, "psaavedra@altosmaipo.ejemplo", null);
        compromiso("CP-2026-0108", "UF-03", "EC-04", "Seguro de hogar octubre", 10, 12, 18_900, "2026-10-01", EstadoCompromiso.PAGADO, 0, null, LocalDateTime.of(2026, 9, 30, 12, 0));
        compromiso("CP-2026-0109", "UF-04", "EC-07", "Arancel octubre", 8, 10, 142_000, "2026-10-05", EstadoCompromiso.EN_REVISION, 0, "snunez@itcordillera.ejemplo", null);
        compromiso("CP-2026-0110", "UF-05", "EC-02", "Mensualidad octubre", 8, 10, 185_000, "2026-10-10", EstadoCompromiso.PENDIENTE, 0, "mfuentes@bosquenativo.ejemplo", null);
        Compromiso anulado = compromiso("CP-2026-0111", "UF-06", "EC-03", "Crédito de consumo", 19, 36, 54_200, "2026-09-20", EstadoCompromiso.ANULADO, 0, "fmorales@creditoandino.ejemplo", null);
        compromiso("CP-2026-0112", "UF-07", "EC-06", "Cuota social octubre", 10, 12, 15_000, "2026-10-05", EstadoCompromiso.PAGADO, 0, "gcarrasco@loshalcones.ejemplo", LocalDateTime.of(2026, 10, 4, 9, 15));
        anular(anulado, MotivoAnulacion.REPACTACION, "fmorales@creditoandino.ejemplo", LocalDateTime.of(2026, 9, 22, 0, 0));

        // 3. septiembre de altos del maipo: con CP-2026-0106 suman 120, 104 pagados y 2 anulados (tasa 88,1%)
        cargarSeptiembre();
    }

    private void cargarSeptiembre() {
        String ejecutiva = "psaavedra@altosmaipo.ejemplo";
        for (int i = 0; i < 119; i++) {
            // 1. 96 pagados a tiempo, 8 con atraso, 2 anulados y 13 vencidos
            boolean conAtraso = i >= 96 && i <= 103;
            boolean anulado = i == 104 || i == 105;
            EstadoCompromiso estado = i <= 103 ? EstadoCompromiso.PAGADO
                    : anulado ? EstadoCompromiso.ANULADO : EstadoCompromiso.VENCIDO;
            LocalDateTime pagadoEl = i > 103 ? null
                    : conAtraso ? LocalDateTime.of(2026, 10, 5, 11, 0) : LocalDateTime.of(2026, 9, 28, 10, 0);

            // 2. el monto con atraso ya trae su recargo en el pago
            Compromiso c = compromiso("CP-2026-%04d".formatted(201 + i), "SEP-%03d".formatted(i + 1), "EC-01",
                    "Cuota pie departamento", 12, 36, conAtraso ? 331_000 : 310_000, "2026-09-30", estado,
                    conAtraso || i >= 106 ? 8_500 : 0, ejecutiva, pagadoEl);
            if (anulado) anular(c, MotivoAnulacion.ERROR_REGISTRO, ejecutiva, LocalDateTime.of(2026, 9, 15, 16, 0));
        }
    }

    // --- helpers privados ---
    private void persona(String codigo, String rut, String nombres, String apellidos, String correo, String telefono) {
        personas.put(codigo, usuarios.porRut(rut).orElseGet(() ->
                usuarios.crear(new CrearPersonaInterna(rut, nombres, apellidos, telefono, correo))));
    }

    private void asignar(String correo, String empresa) {
        ejecutivoService.asignar(ejecutivos.get(correo), empresa);
    }

    private Compromiso compromiso(String folio, String usuario, String empresa, String concepto,
                                  Integer cuota, Integer totalCuotas, long monto, String vence,
                                  EstadoCompromiso estado, long recargo, String ejecutivo, LocalDateTime pagadoEl) {
        PersonaInterna p = personas.get(usuario);
        EmpresaCliente emp = empresaRepo.findByCodigo(empresa).orElseThrow();
        Compromiso c = compromisoRepo.save(Compromiso.builder()
                .folio(folio)
                .empresa(emp)
                .personaId(p.id())
                .personaRut(p.rut())
                .personaNombre(p.nombres() + " " + p.apellidos())
                .concepto(concepto)
                .numeroCuota(cuota)
                .totalCuotas(totalCuotas)
                .monto(monto)
                .fechaVencimiento(LocalDate.parse(vence))
                .estado(estado)
                .recargoAplicado(recargo)
                .fechaCambioEstado(pagadoEl == null ? CARGA : pagadoEl)
                // sin ejecutivo en el anexo queda como carga inicial
                .registradoPorId(ejecutivo == null ? null : ejecutivos.get(ejecutivo))
                .registradoPorCorreo(ejecutivo == null ? "carga-inicial@pagoflex.ejemplo" : ejecutivo)
                .fechaRegistro(CARGA)
                .build());
        if (pagadoEl != null) pagar(c, emp, pagadoEl);
        return c;
    }

    private void pagar(Compromiso c, EmpresaCliente emp, LocalDateTime fecha) {
        // los pagados del anexo también tienen su comprobante para el historial
        long total = c.getMonto() + c.getRecargoAplicado();
        Pago pago = Pago.builder()
                .compromiso(c)
                .fechaHora(fecha)
                .montoPagado(total)
                .canal(CanalPago.PORTAL)
                .medio(MedioPago.TRANSFERENCIA)
                .build();
        pago.setComprobante(Comprobante.builder()
                .numero("CPR-%06d".formatted(++comprobantes))
                .pago(pago)
                .folio(c.getFolio())
                .empresa(emp.getNombreFantasia())
                .concepto(c.getConcepto())
                .numeroCuota(c.getNumeroCuota())
                .totalCuotas(c.getTotalCuotas())
                .personaNombre(c.getPersonaNombre())
                .personaRut(c.getPersonaRut())
                .fechaVencimiento(c.getFechaVencimiento())
                .monto(c.getMonto())
                .recargo(c.getRecargoAplicado())
                .total(total)
                .canal(CanalPago.PORTAL.name())
                .medio(MedioPago.TRANSFERENCIA.name())
                .fechaPago(fecha)
                .build());
        pagoRepo.save(pago);
    }

    private void anular(Compromiso c, MotivoAnulacion motivo, String ejecutivo, LocalDateTime fecha) {
        anulacionRepo.save(Anulacion.builder()
                .compromiso(c).motivo(motivo)
                .ejecutivoId(ejecutivos.get(ejecutivo)).ejecutivoCorreo(ejecutivo)
                .fecha(fecha)
                .build());
    }

    private void dormir() {
        try {
            Thread.sleep(esperaMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    // módulo 11 del rut chileno
    private static String digitoVerificador(String cuerpo) {
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int dv = 11 - (suma % 11);
        return dv == 11 ? "0" : dv == 10 ? "K" : String.valueOf(dv);
    }
}
