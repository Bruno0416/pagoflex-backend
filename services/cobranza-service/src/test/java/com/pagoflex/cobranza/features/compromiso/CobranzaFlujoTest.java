package com.pagoflex.cobranza.features.compromiso;

import com.pagoflex.cobranza.features.compromiso.entity.Compromiso;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.entity.Persona;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.repository.PersonaRepository;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.cobranza.support.RelojFijo;
import com.pagoflex.cobranza.support.TokensPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Flujo del ejecutivo de punta a punta sobre una base H2, con la fecha fija del caso
@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojFijo.class)
@Transactional
class CobranzaFlujoTest {
    private static final String CLAVE = "X-Internal-Api-Key";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CompromisoRepository compromisoRepo;
    @Autowired
    private PersonaRepository personaRepo;
    @Autowired
    private EmpresaClienteRepository empresaRepo;

    private final UUID paula = UUID.randomUUID();
    private final UUID sebastian = UUID.randomUUID();
    private String tokenPaula;
    private String tokenSebastian;

    @BeforeEach
    void asignarEmpresas() throws Exception {
        // 1. paula es ejecutiva de altos del maipo y sebastián del instituto
        asignar("psaavedra@altosmaipo.ejemplo", "EC-01");
        asignar("snunez@itcordillera.ejemplo", "EC-07");
        tokenPaula = TokensPrueba.bearer(paula, "psaavedra@altosmaipo.ejemplo", "EJECUTIVO");
        tokenSebastian = TokensPrueba.bearer(sebastian, "snunez@itcordillera.ejemplo", "EJECUTIVO");
    }

    // ---------- seguridad ----------
    @Test
    void cobranzaRequiresExecutiveRole() throws Exception {
        mvc.perform(get("/api/v1/cobranza/resumen").param("mes", "2026-10"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/cobranza/resumen").param("mes", "2026-10")
                        .header("Authorization", TokensPrueba.bearer(UUID.randomUUID(), "crojas@correo.ejemplo", "USUARIO_FINAL")))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalRoutesRequireTheSharedKey() throws Exception {
        mvc.perform(get("/api/v1/internal/cobranza/compromisos/CP-2026-0001"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/internal/cobranza/compromisos/CP-2026-0001").header(CLAVE, "otra-clave"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void executiveWithoutCompanyIsRejected() throws Exception {
        mvc.perform(get("/api/v1/cobranza/resumen").param("mes", "2026-10")
                        .header("Authorization", TokensPrueba.bearer(UUID.randomUUID(), "otro@ejemplo.cl", "EJECUTIVO")))
                .andExpect(status().isForbidden());
    }

    // ---------- registro RN-20 ----------
    @Test
    void registerNewPersonCreatesPendingCommitment() throws Exception {
        registrar(tokenPaula, compromisoNuevo("Gastos comunes noviembre", 16))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio").value("CP-2026-0001"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.persona.rut").value("12476318-5"))
                .andExpect(jsonPath("$.empresa").value("Altos del Maipo"))
                .andExpect(jsonPath("$.registradoPor").value("psaavedra@altosmaipo.ejemplo"));
    }

    @Test
    void newPersonNeedsBasicData() throws Exception {
        registrar(tokenPaula, """
                {"rut":"12.476.318-5","concepto":"Gastos comunes","monto":45000,"fechaVencimiento":"2026-11-30"}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void sameCommitmentCannotBeRegisteredTwice() throws Exception {
        registrar(tokenPaula, compromisoNuevo("Cuota pie", 16)).andExpect(status().isCreated());
        registrar(tokenPaula, compromisoNuevo("cuota PIE", 16)).andExpect(status().isConflict());
        registrar(tokenPaula, compromisoNuevo("Cuota pie", 17)).andExpect(status().isCreated());
    }

    @Test
    void invalidDataIsRejected() throws Exception {
        registrar(tokenPaula, compromisoNuevo("Cuota", 16).replace("12.476.318-5", "12.345.678-9"))
                .andExpect(status().isBadRequest());
        registrar(tokenPaula, compromisoNuevo("Cuota", 16).replace("2026-11-30", "2026-10-01"))
                .andExpect(status().isBadRequest());
        registrar(tokenPaula, compromisoNuevo("Cuota", 40)).andExpect(status().isBadRequest());
        registrar(tokenPaula, compromisoNuevo("Cuota", 16).replace("45000", "0")).andExpect(status().isBadRequest());
    }

    // ---------- visibilidad RN-22 ----------
    @Test
    void executiveCannotSeeAnotherCompany() throws Exception {
        registrar(tokenPaula, compromisoNuevo("Cuota pie", 16)).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/cobranza/compromisos/CP-2026-0001").header("Authorization", tokenSebastian))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/cobranza/compromisos/CP-2026-0001/anulacion").header("Authorization", tokenSebastian)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"REPACTACION\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/cobranza/compromisos").param("mes", "2026-11").header("Authorization", tokenSebastian))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- resumen RN-24 ----------
    @Test
    void monthlySummaryCountsByStateAndRate() throws Exception {
        EmpresaCliente altos = empresaRepo.findByCodigo("EC-01").orElseThrow();
        Persona persona = persona();
        for (int i = 1; i <= 3; i++) guardar(altos, persona, "Pagado " + i, EstadoCompromiso.PAGADO, LocalDate.of(2026, 9, 10));
        guardar(altos, persona, "Anulado", EstadoCompromiso.ANULADO, LocalDate.of(2026, 9, 12));
        guardar(altos, persona, "Atrasado", EstadoCompromiso.PENDIENTE, LocalDate.of(2026, 9, 30));

        mvc.perform(get("/api/v1/cobranza/resumen").param("mes", "2026-09").header("Authorization", tokenPaula))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompromisos").value(5))
                .andExpect(jsonPath("$.pagados").value(3))
                .andExpect(jsonPath("$.anulados").value(1))
                .andExpect(jsonPath("$.vencidos").value(1))
                .andExpect(jsonPath("$.tasaCobranza").value(75.0))
                .andExpect(jsonPath("$.recaudado").value(300_000));

        mvc.perform(get("/api/v1/cobranza/compromisos").param("mes", "2026-09").param("estado", "VENCIDO")
                        .header("Authorization", tokenPaula))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recargo").value(8_500))
                .andExpect(jsonPath("$[0].diasAtraso").value(6));
    }

    // ---------- anulación RN-21 ----------
    @Test
    void cancellationRules() throws Exception {
        EmpresaCliente altos = empresaRepo.findByCodigo("EC-01").orElseThrow();
        Compromiso pagado = guardar(altos, persona(), "Pagado", EstadoCompromiso.PAGADO, LocalDate.of(2026, 9, 10));
        registrar(tokenPaula, compromisoNuevo("Cuota pie", 16)).andExpect(status().isCreated());

        anular(pagado.getFolio(), "{\"motivo\":\"ERROR_REGISTRO\"}").andExpect(status().isConflict());
        anular("CP-2026-0001", "{\"motivo\":\"OTRO\",\"comentario\":\"  \"}").andExpect(status().isConflict());
        anular("CP-2026-0001", "{\"motivo\":\"REPACTACION\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"))
                .andExpect(jsonPath("$.anulacion.motivo").value("REPACTACION"))
                .andExpect(jsonPath("$.anulacion.ejecutivo").value("psaavedra@altosmaipo.ejemplo"));
        anular("CP-2026-0001", "{\"motivo\":\"REPACTACION\"}").andExpect(status().isConflict());
    }

    // ---------- apoyo a las pantallas ----------
    @Test
    void executiveSeesHisCompanyAndFindsPeople() throws Exception {
        mvc.perform(get("/api/v1/cobranza/empresa").header("Authorization", tokenPaula))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("EC-01"))
                .andExpect(jsonPath("$.nombre").value("Altos del Maipo"));

        mvc.perform(get("/api/v1/cobranza/personas/12.476.318-5").header("Authorization", tokenPaula))
                .andExpect(status().isNotFound());
        registrar(tokenPaula, compromisoNuevo("Cuota pie", 16)).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/cobranza/personas/12.476.318-5").header("Authorization", tokenPaula))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luis Alberto Muñoz Tapia"))
                .andExpect(jsonPath("$.correo").value("lmunoz@correo.ejemplo"));
        mvc.perform(get("/api/v1/cobranza/personas/12.345.678-9").header("Authorization", tokenPaula))
                .andExpect(status().isBadRequest());
    }

    // ---------- usuario final ----------
    @Test
    void finalUserSeesOnlyHisCommitments() throws Exception {
        EmpresaCliente altos = empresaRepo.findByCodigo("EC-01").orElseThrow();
        guardar(altos, persona(), "Cuota de camila", EstadoCompromiso.PENDIENTE, LocalDate.of(2026, 10, 30));
        registrar(tokenPaula, compromisoNuevo("Cuota de luis", 16)).andExpect(status().isCreated());

        String camila = TokensPrueba.bearer(UUID.randomUUID(), "CRojas@correo.ejemplo", "USUARIO_FINAL");
        mvc.perform(get("/api/v1/cobranza/mis-compromisos").header("Authorization", camila))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].concepto").value("Cuota de camila"))
                .andExpect(jsonPath("$[0].empresa").value("Altos del Maipo"));

        // el ejecutivo no usa esta ruta y sin mes ve todos los de su empresa
        mvc.perform(get("/api/v1/cobranza/mis-compromisos").header("Authorization", tokenPaula))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/cobranza/compromisos").header("Authorization", tokenPaula))
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------- revisión desde soporte RN-09 ----------
    @Test
    void reviewSavesSurchargeAndReturnsByDate() throws Exception {
        EmpresaCliente altos = empresaRepo.findByCodigo("EC-01").orElseThrow();
        Compromiso atrasado = guardar(altos, persona(), "Atrasado", EstadoCompromiso.PENDIENTE, LocalDate.of(2026, 9, 30));
        Compromiso pagado = guardar(altos, persona(), "Pagado", EstadoCompromiso.PAGADO, LocalDate.of(2026, 9, 10));

        mvc.perform(post("/api/v1/internal/cobranza/compromisos/" + atrasado.getFolio() + "/revision").header(CLAVE, TokensPrueba.CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.total").value(108_500));
        mvc.perform(delete("/api/v1/internal/cobranza/compromisos/" + atrasado.getFolio() + "/revision").header(CLAVE, TokensPrueba.CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VENCIDO"))
                .andExpect(jsonPath("$.total").value(108_500));
        mvc.perform(post("/api/v1/internal/cobranza/compromisos/" + pagado.getFolio() + "/revision").header(CLAVE, TokensPrueba.CLAVE_INTERNA))
                .andExpect(status().isConflict());
    }

    // --- helpers privados ---
    private void asignar(String correo, String empresa) throws Exception {
        mvc.perform(put("/api/v1/internal/cobranza/ejecutivos").header(CLAVE, TokensPrueba.CLAVE_INTERNA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"codigoEmpresa\":\"" + empresa + "\"}"))
                .andExpect(status().isNoContent());
    }

    private org.springframework.test.web.servlet.ResultActions registrar(String token, String json) throws Exception {
        return mvc.perform(post("/api/v1/cobranza/compromisos").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private org.springframework.test.web.servlet.ResultActions anular(String folio, String json) throws Exception {
        return mvc.perform(post("/api/v1/cobranza/compromisos/" + folio + "/anulacion").header("Authorization", tokenPaula)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String compromisoNuevo(String concepto, int cuota) {
        return """
                {"rut":"12.476.318-5","nombres":"Luis Alberto","apellidos":"Muñoz Tapia",
                 "correo":"lmunoz@correo.ejemplo","telefono":"+56 9 0000 0102",
                 "concepto":"%s","monto":45000,"fechaVencimiento":"2026-11-30","numeroCuota":%d,"totalCuotas":36}
                """.formatted(concepto, cuota);
    }

    private Persona persona() {
        return personaRepo.findByRut("15834207-3").orElseGet(() -> personaRepo.save(Persona.builder()
                .rut("15834207-3").nombres("Camila Andrea").apellidos("Rojas Peña")
                .correo("crojas@correo.ejemplo").telefono("+56 9 0000 0101").build()));
    }

    private Compromiso guardar(EmpresaCliente empresa, Persona persona, String concepto, EstadoCompromiso estado, LocalDate vence) {
        return compromisoRepo.save(Compromiso.builder()
                .folio("PR-" + (compromisoRepo.count() + 1))
                .empresa(empresa).persona(persona).concepto(concepto)
                .monto(100_000).fechaVencimiento(vence).estado(estado).recargoAplicado(0)
                .fechaCambioEstado(RelojFijo.AHORA).registradoPor("psaavedra@altosmaipo.ejemplo").fechaRegistro(RelojFijo.AHORA)
                .build());
    }
}
