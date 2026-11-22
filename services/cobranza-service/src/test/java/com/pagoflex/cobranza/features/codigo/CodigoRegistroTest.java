package com.pagoflex.cobranza.features.codigo;

import com.pagoflex.cobranza.features.codigo.entity.CodigoRegistro;
import com.pagoflex.cobranza.features.codigo.repository.CodigoRegistroRepository;
import com.pagoflex.cobranza.features.codigo.service.CodigoRegistroService;
import com.pagoflex.cobranza.features.codigo.service.GeneradorCodigo;
import com.pagoflex.cobranza.features.empresa.data.CargaEmpresas;
import com.pagoflex.cobranza.features.empresa.exceptions.EmpresaNoEncontrada;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.cobranza.support.RedisPrueba;
import com.pagoflex.cobranza.support.RelojFijo;
import com.pagoflex.contracts.organizacion.RolAutorizado;
import com.pagoflex.contracts.organizacion.IdentificadoresEmpresa;
import com.pagoflex.contracts.organizacion.TipoOrganizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.UUID;

import static com.pagoflex.cobranza.support.TokensPrueba.CLAVE_INTERNA;
import static com.pagoflex.cobranza.support.TokensPrueba.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Contrato POST /api/v1/internal/empresas/codigos/validar con códigos reales guardados con BCrypt
@SpringBootTest
@AutoConfigureMockMvc
@Import({RelojFijo.class, RedisPrueba.class})
class CodigoRegistroTest {
    private static final String RUTA = "/api/v1/internal/empresas/codigos/validar";
    private static final Instant PASADO = Instant.parse("2020-01-01T00:00:00Z");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CodigoRegistroService service;
    @Autowired
    private CodigoRegistroRepository codigoRepo;
    @Autowired
    private EmpresaClienteRepository empresaRepo;
    @Autowired
    private PasswordEncoder encoder;

    private UUID empresaAltos;

    @BeforeEach
    void preparar() {
        codigoRepo.deleteAll();
        new CargaEmpresas(empresaRepo).run(null);
        empresaAltos = empresaRepo.findByCodigo("EC-01").orElseThrow().getId();
    }

    // ---------- códigos válidos ----------
    @Test
    void codigoDeLaOrganizacionInternaAutorizaAgente() throws Exception {
        String codigo = service.generarParaAgente(null);

        mvc.perform(validar(codigo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.idEmpresa").value(IdentificadoresEmpresa.INTERNA.toString()))
                .andExpect(jsonPath("$.nombreEmpresa").value("PagoFlex"))
                .andExpect(jsonPath("$.tipoEmpresa").value("INTERNA"))
                .andExpect(jsonPath("$.rolAutorizado").value("AGENTE"))
                // exactamente los cuatro campos del contrato
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void codigoDeEmpresaClienteAutorizaEjecutivoDeEsaEmpresa() throws Exception {
        String codigo = service.generarParaEjecutivo(empresaAltos, null);

        mvc.perform(validar(codigo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.idEmpresa").value(empresaAltos.toString()))
                .andExpect(jsonPath("$.nombreEmpresa").value("Altos del Maipo"))
                .andExpect(jsonPath("$.tipoEmpresa").value("CLIENTE"))
                .andExpect(jsonPath("$.rolAutorizado").value("EJECUTIVO"))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void elRolLoDecideElServidorYNoElCuerpoDeLaPeticion() throws Exception {
        String codigo = service.generarParaEjecutivo(empresaAltos, null);

        mvc.perform(post(RUTA).header("X-Internal-Api-Key", CLAVE_INTERNA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"rolAutorizado\":\"AGENTE\",\"tipoEmpresa\":\"INTERNA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolAutorizado").value("EJECUTIVO"))
                .andExpect(jsonPath("$.tipoEmpresa").value("CLIENTE"));
    }

    @Test
    void elCodigoEsReutilizableDentroDeSuVigencia() throws Exception {
        String codigo = service.generarParaEjecutivo(empresaAltos, Instant.parse("2030-01-01T00:00:00Z"));

        mvc.perform(validar(codigo)).andExpect(status().isOk());
        mvc.perform(validar(codigo)).andExpect(status().isOk());
    }

    // ---------- almacenamiento ----------
    @Test
    void elCodigoNoSeGuardaEnTextoPlanoSinoComoHashBcrypt() {
        String codigo = service.generarParaAgente(null);

        CodigoRegistro guardado = codigoRepo.findByPrefijo(GeneradorCodigo.prefijoDe(codigo)).orElseThrow();
        String secreto = codigo.substring(7);
        assertThat(guardado.getHash()).startsWith("$2").doesNotContain(codigo).doesNotContain(secreto);
        assertThat(encoder.matches(codigo, guardado.getHash())).isTrue();
        assertThat(codigoRepo.findAll()).allSatisfy(c ->
                assertThat(c.getPrefijo()).hasSize(6).matches("[A-Z]{2}[0-9]{2}[A-Z]{2}"));
    }

    @Test
    void dosCodigosGeneradosSonDistintos() {
        String uno = service.generarParaAgente(null);
        String otro = service.generarParaAgente(null);

        assertThat(uno).isNotEqualTo(otro).matches("^[A-Z]{2}[0-9]{2}[A-Z]{2}-[A-Z2-7]{26}$");
        assertThat(codigoRepo.count()).isEqualTo(2);
    }

    @Test
    void noSeGeneraUnCodigoDeEjecutivoParaUnaEmpresaInexistente() {
        assertThatThrownBy(() -> service.generarParaEjecutivo(UUID.randomUUID(), null))
                .isInstanceOf(EmpresaNoEncontrada.class);
        assertThatThrownBy(() -> service.generarParaEjecutivo(null, null)).isInstanceOf(EmpresaNoEncontrada.class);
    }

    // ---------- rechazos ----------
    @Test
    void formatoInvalidoSeRechazaConBadRequest() throws Exception {
        for (String codigo : new String[]{"AB12CD", "AB12CD-CORTO", "ab12cd-ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                "AB12CD-ABCDEFGHIJKLMNOPQRSTUVWX01", "AB12CD-ABCDEFGHIJKLMNOPQRSTUVWXYZ1", " "}) {
            mvc.perform(validar(codigo)).andExpect(status().isBadRequest());
        }
        mvc.perform(post(RUTA).header("X-Internal-Api-Key", CLAVE_INTERNA).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    void inexistenteIncorrectoRevocadoYExpiradoResponden404Igual() throws Exception {
        String inexistente = "ZZ99ZZ-" + GeneradorCodigo.secretoAleatorio();
        String valido = service.generarParaEjecutivo(empresaAltos, null);
        String incorrecto = GeneradorCodigo.prefijoDe(valido) + "-" + GeneradorCodigo.secretoAleatorio();
        String revocado = service.generarParaEjecutivo(empresaAltos, null);
        service.revocar(GeneradorCodigo.prefijoDe(revocado));
        String expirado = service.generarParaEjecutivo(empresaAltos, PASADO);

        // misma respuesta (el timestamp del error es lo único que cambia)
        for (String codigo : new String[]{inexistente, incorrecto, revocado, expirado}) {
            mvc.perform(validar(codigo)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("El código de empresa no es válido"))
                    .andExpect(jsonPath("$.title").value("Error en la solicitud"))
                    .andExpect(jsonPath("$.status").value(404));
        }
        // el válido sigue funcionando
        mvc.perform(validar(valido)).andExpect(status().isOk());
    }

    @Test
    void combinacionesDeOrganizacionYRolNoPermitidasSeRechazan() throws Exception {
        String interEjecutivo = guardarManual(TipoOrganizacion.INTERNA, RolAutorizado.EJECUTIVO,
                IdentificadoresEmpresa.INTERNA);
        String clienteAgente = guardarManual(TipoOrganizacion.CLIENTE, RolAutorizado.AGENTE, empresaAltos);
        String internaOtraOrg = guardarManual(TipoOrganizacion.INTERNA, RolAutorizado.AGENTE, empresaAltos);
        String clienteInexistente = guardarManual(TipoOrganizacion.CLIENTE, RolAutorizado.EJECUTIVO, UUID.randomUUID());

        for (String codigo : new String[]{interEjecutivo, clienteAgente, internaOtraOrg, clienteInexistente}) {
            mvc.perform(validar(codigo)).andExpect(status().isNotFound());
        }
    }

    // ---------- autenticación entre servicios ----------
    @Test
    void sinClaveInternaOConClaveIncorrectaNoResponde() throws Exception {
        String codigo = service.generarParaAgente(null);
        String cuerpo = "{\"codigo\":\"" + codigo + "\"}";

        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(RUTA).header("X-Internal-Api-Key", "otra").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        // un jwt no sustituye la clave de servicio
        mvc.perform(post(RUTA).header("Authorization", bearer(UUID.randomUUID(), "a@b.cl", "AGENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laRespuestaNoIncluyeHashesNiElSecreto() throws Exception {
        String codigo = service.generarParaAgente(null);
        String hash = codigoRepo.findByPrefijo(GeneradorCodigo.prefijoDe(codigo)).orElseThrow().getHash();

        String cuerpo = mvc.perform(validar(codigo)).andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain(hash).doesNotContain(codigo.substring(7)).doesNotContain("hash");
    }

    // --- helpers ---
    private MockHttpServletRequestBuilder validar(String codigo) {
        return post(RUTA).header("X-Internal-Api-Key", CLAVE_INTERNA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + codigo + "\"}");
    }

    // inserta un registro con hash válido pero con una combinación que el generador nunca produce
    private String guardarManual(TipoOrganizacion tipo, RolAutorizado rol, UUID empresaId) {
        String prefijo = GeneradorCodigo.prefijoAleatorio();
        String codigo = GeneradorCodigo.armar(prefijo, GeneradorCodigo.secretoAleatorio());
        codigoRepo.save(CodigoRegistro.builder().prefijo(prefijo).hash(encoder.encode(codigo)).empresaId(empresaId)
                .tipo(tipo).rol(rol).activo(true).creadoEn(Instant.now()).build());
        return codigo;
    }
}
