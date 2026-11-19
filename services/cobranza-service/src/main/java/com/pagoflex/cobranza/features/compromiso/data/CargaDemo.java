package com.pagoflex.cobranza.features.compromiso.data;

import com.pagoflex.cobranza.features.compromiso.entity.*;
import com.pagoflex.cobranza.features.compromiso.repository.AnulacionRepository;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.repository.PersonaRepository;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.cobranza.features.empresa.service.EjecutivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/// Datos de ejemplo del anexo 9, los mismos que trae la app para probarla conectada.
/// Solo se cargan con pagoflex.demo.enabled=true y si no hay compromisos
@Component
@Order(2)
@ConditionalOnProperty(name = "pagoflex.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CargaDemo implements ApplicationRunner {
    private static final LocalDateTime CARGA = LocalDateTime.of(2026, 8, 30, 9, 0);

    private final EmpresaClienteRepository empresaRepo;
    private final PersonaRepository personaRepo;
    private final CompromisoRepository compromisoRepo;
    private final AnulacionRepository anulacionRepo;
    private final EjecutivoService ejecutivoService;

    private final Map<String, Persona> personas = new HashMap<>();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1. no repetir la carga
        if (compromisoRepo.count() > 0) return;

        // 2. cada ejecutivo con su empresa
        ejecutivoService.asignar("psaavedra@altosmaipo.ejemplo", "EC-01");
        ejecutivoService.asignar("mfuentes@bosquenativo.ejemplo", "EC-02");
        ejecutivoService.asignar("chenriquez@creditoandino.ejemplo", "EC-03");
        ejecutivoService.asignar("fmorales@creditoandino.ejemplo", "EC-03");
        ejecutivoService.asignar("gcarrasco@loshalcones.ejemplo", "EC-06");
        ejecutivoService.asignar("snunez@itcordillera.ejemplo", "EC-07");
        ejecutivoService.asignar("mleiva@plazaoriente.ejemplo", "EC-08");

        // 3. usuarios finales
        persona("UF-01", "15834207-3", "Camila Andrea", "Rojas Peña", "crojas@correo.ejemplo", "+56 9 0000 0101");
        persona("UF-02", "12476318-5", "Luis Alberto", "Muñoz Tapia", "lmunoz@correo.ejemplo", "+56 9 0000 0102");
        persona("UF-03", "6893541-5", "Rosa Elena", "Contreras Díaz", "rcontreras@correo.ejemplo", "+56 9 0000 0103");
        persona("UF-04", "21045873-5", "Matías Ignacio", "Soto Vergara", "msoto@correo.ejemplo", "+56 9 0000 0104");
        persona("UF-05", "17562904-1", "Fernanda Paz", "Godoy Lagos", "fgodoy@correo.ejemplo", "+56 9 0000 0105");
        persona("UF-06", "10318265-4", "Héctor Manuel", "Pizarro Campos", "hpizarro@correo.ejemplo", "+56 9 0000 0106");
        persona("UF-07", "19287430-0", "Javiera Constanza", "Reyes Molina", "jreyes@correo.ejemplo", "+56 9 0000 0107");
        persona("UF-08", "18704396-4", "Benjamín Esteban", "Araya Fuentes", "baraya@correo.ejemplo", "+56 9 0000 0108");

        // 4. compromisos de octubre
        compromiso("CP-2026-0101", "UF-01", "EC-08", "Arriendo departamento octubre", 10, 12, 520_000, "2026-10-10", EstadoCompromiso.PENDIENTE, 0, "mleiva@plazaoriente.ejemplo");
        compromiso("CP-2026-0102", "UF-01", "EC-06", "Cuota social octubre", 10, 12, 15_000, "2026-10-05", EstadoCompromiso.VENCIDO, 2_000, "gcarrasco@loshalcones.ejemplo");
        compromiso("CP-2026-0103", "UF-01", "EC-05", "Aporte mensual octubre", null, null, 10_000, "2026-10-15", EstadoCompromiso.PENDIENTE, 0, null);
        compromiso("CP-2026-0104", "UF-01", "EC-03", "Crédito de consumo", 7, 24, 86_400, "2026-09-25", EstadoCompromiso.PAGADO, 0, "chenriquez@creditoandino.ejemplo");
        compromiso("CP-2026-0105", "UF-01", "EC-03", "Crédito de consumo", 8, 24, 86_400, "2026-10-25", EstadoCompromiso.PENDIENTE, 0, "chenriquez@creditoandino.ejemplo");
        compromiso("CP-2026-0106", "UF-02", "EC-01", "Cuota pie departamento", 14, 36, 310_000, "2026-09-30", EstadoCompromiso.EN_REVISION, 8_500, "psaavedra@altosmaipo.ejemplo");
        compromiso("CP-2026-0107", "UF-02", "EC-01", "Cuota pie departamento", 15, 36, 310_000, "2026-10-30", EstadoCompromiso.PENDIENTE, 0, "psaavedra@altosmaipo.ejemplo");
        compromiso("CP-2026-0108", "UF-03", "EC-04", "Seguro de hogar octubre", 10, 12, 18_900, "2026-10-01", EstadoCompromiso.PAGADO, 0, null);
        compromiso("CP-2026-0109", "UF-04", "EC-07", "Arancel octubre", 8, 10, 142_000, "2026-10-05", EstadoCompromiso.EN_REVISION, 0, "snunez@itcordillera.ejemplo");
        compromiso("CP-2026-0110", "UF-05", "EC-02", "Mensualidad octubre", 8, 10, 185_000, "2026-10-10", EstadoCompromiso.PENDIENTE, 0, "mfuentes@bosquenativo.ejemplo");
        Compromiso anulado = compromiso("CP-2026-0111", "UF-06", "EC-03", "Crédito de consumo", 19, 36, 54_200, "2026-09-20", EstadoCompromiso.ANULADO, 0, "fmorales@creditoandino.ejemplo");
        compromiso("CP-2026-0112", "UF-07", "EC-06", "Cuota social octubre", 10, 12, 15_000, "2026-10-05", EstadoCompromiso.PAGADO, 0, "gcarrasco@loshalcones.ejemplo");
        anular(anulado, MotivoAnulacion.REPACTACION, "fmorales@creditoandino.ejemplo", LocalDateTime.of(2026, 9, 22, 0, 0));

        // 5. septiembre de altos del maipo: con CP-2026-0106 suman 120, 104 pagados y 2 anulados (tasa 88,1%)
        cargarSeptiembre();
    }

    // --- helpers privados ---
    private void cargarSeptiembre() {
        EmpresaCliente altos = empresaRepo.findByCodigo("EC-01").orElseThrow();
        String ejecutiva = "psaavedra@altosmaipo.ejemplo";

        for (int i = 0; i < 119; i++) {
            // 1. persona sintética con rut válido
            String cuerpo = String.valueOf(20_100_000 + (i + 1) * 137);
            String numero = "%03d".formatted(i + 1);
            Persona persona = personaRepo.save(Persona.builder()
                    .rut(cuerpo + "-" + digitoVerificador(cuerpo))
                    .nombres("Cliente")
                    .apellidos("Demo " + numero)
                    .correo("cliente" + numero + "@correo.ejemplo")
                    .telefono("+56 9 0000 %04d".formatted(1000 + i))
                    .build());

            // 2. 96 pagados a tiempo, 8 con atraso, 2 anulados y 13 vencidos
            boolean conAtraso = i >= 96 && i <= 103;
            boolean anulado = i == 104 || i == 105;
            EstadoCompromiso estado = i <= 103 ? EstadoCompromiso.PAGADO
                    : anulado ? EstadoCompromiso.ANULADO : EstadoCompromiso.VENCIDO;

            Compromiso c = compromisoRepo.save(Compromiso.builder()
                    .folio("CP-2026-%04d".formatted(201 + i))
                    .empresa(altos)
                    .persona(persona)
                    .concepto("Cuota pie departamento")
                    .numeroCuota(12)
                    .totalCuotas(36)
                    .monto(conAtraso ? 331_000 : 310_000)
                    .fechaVencimiento(LocalDate.of(2026, 9, 30))
                    .estado(estado)
                    .recargoAplicado(conAtraso || i >= 106 ? 8_500 : 0)
                    .fechaCambioEstado(CARGA)
                    .registradoPor(ejecutiva)
                    .fechaRegistro(CARGA)
                    .build());
            if (anulado) {
                anular(c, MotivoAnulacion.ERROR_REGISTRO, ejecutiva, LocalDateTime.of(2026, 9, 15, 16, 0));
            }
        }
    }

    private void persona(String codigo, String rut, String nombres, String apellidos, String correo, String telefono) {
        personas.put(codigo, personaRepo.save(Persona.builder()
                .rut(rut).nombres(nombres).apellidos(apellidos).correo(correo).telefono(telefono)
                .build()));
    }

    private Compromiso compromiso(String folio, String usuario, String empresa, String concepto,
                                  Integer cuota, Integer totalCuotas, long monto, String vence,
                                  EstadoCompromiso estado, long recargo, String ejecutivo) {
        return compromisoRepo.save(Compromiso.builder()
                .folio(folio)
                .empresa(empresaRepo.findByCodigo(empresa).orElseThrow())
                .persona(personas.get(usuario))
                .concepto(concepto)
                .numeroCuota(cuota)
                .totalCuotas(totalCuotas)
                .monto(monto)
                .fechaVencimiento(LocalDate.parse(vence))
                .estado(estado)
                .recargoAplicado(recargo)
                .fechaCambioEstado(CARGA)
                // sin ejecutivo en el anexo queda como carga inicial
                .registradoPor(ejecutivo == null ? "carga-inicial@pagoflex.ejemplo" : ejecutivo)
                .fechaRegistro(CARGA)
                .build());
    }

    private void anular(Compromiso c, MotivoAnulacion motivo, String ejecutivo, LocalDateTime fecha) {
        anulacionRepo.save(Anulacion.builder()
                .compromiso(c).motivo(motivo).ejecutivo(ejecutivo).fecha(fecha)
                .build());
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
