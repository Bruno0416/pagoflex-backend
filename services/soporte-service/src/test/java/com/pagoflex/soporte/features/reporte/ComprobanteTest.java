package com.pagoflex.soporte.features.reporte;

import com.pagoflex.soporte.core.client.CobranzaServiceClient;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.entity.MotivoReporte;
import com.pagoflex.soporte.features.reporte.entity.Reporte;
import com.pagoflex.soporte.features.reporte.repository.ReporteRepository;
import com.pagoflex.soporte.support.RedisPrueba;
import com.pagoflex.soporte.support.RelojFijo;
import com.pagoflex.soporte.support.TokensPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// La usuaria adjunta la foto del comprobante y el agente la revisa
@SpringBootTest
@AutoConfigureMockMvc
@Import({RelojFijo.class, RedisPrueba.class})
@Transactional
class ComprobanteTest {
    private static final byte[] FOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ReporteRepository repository;

    @MockitoBean
    private CobranzaServiceClient cobranza;

    private final UUID camila = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private String tokenCamila;
    private String tokenAna;
    private String tokenOtraUsuaria;

    @BeforeEach
    void preparar() {
        tokenCamila = TokensPrueba.bearer(camila, "crojas@correo.ejemplo", "USUARIO_FINAL");
        tokenAna = TokensPrueba.bearer(ana, "aperez@pagoflex.ejemplo", "AGENTE");
        tokenOtraUsuaria = TokensPrueba.bearer(UUID.randomUUID(), "otra@correo.ejemplo", "USUARIO_FINAL");
        guardar("REP-0001");
    }

    @Test
    void userUploadsTheReceiptAndTheAgentSeesIt() throws Exception {
        mvc.perform(get("/api/v1/reportes/REP-0001").header("Authorization", tokenAna))
                .andExpect(jsonPath("$.tieneComprobante").value(false));

        subir(tokenCamila, "image/jpeg", FOTO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneComprobante").value(true));

        mvc.perform(get("/api/v1/reportes/REP-0001/comprobante").header("Authorization", tokenAna))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(FOTO));
    }

    @Test
    void onlyImagesAreAccepted() throws Exception {
        subir(tokenCamila, "application/pdf", FOTO).andExpect(status().isBadRequest());
        subir(tokenCamila, "image/png", new byte[0]).andExpect(status().isBadRequest());
    }

    @Test
    void anotherUserCannotUploadToMyReport() throws Exception {
        subir(tokenOtraUsuaria, "image/jpeg", FOTO).andExpect(status().isNotFound());
    }

    @Test
    void reportWithoutReceiptIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/reportes/REP-0001/comprobante").header("Authorization", tokenAna))
                .andExpect(status().isNotFound());
    }

    @Test
    void agentCannotSeeTheReceiptOfAnotherAgentsReport() throws Exception {
        subir(tokenCamila, "image/png", FOTO).andExpect(status().isOk());
        Reporte r = repository.findByNumero("REP-0001").orElseThrow();
        r.setEstado(EstadoReporte.ASIGNADO);
        r.setAgenteId(UUID.randomUUID());

        mvc.perform(get("/api/v1/reportes/REP-0001/comprobante").header("Authorization", tokenAna))
                .andExpect(status().isNotFound());
    }

    private ResultActions subir(String token, String tipo, byte[] datos) throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("file", "comprobante", tipo, datos);
        return mvc.perform(multipart(HttpMethod.PUT, "/api/v1/reportes/me/REP-0001/comprobante")
                .file(archivo).header("Authorization", token));
    }

    private void guardar(String numero) {
        LocalDateTime creacion = LocalDateTime.of(2026, 10, 5, 9, 0);
        Reporte r = Reporte.builder()
                .numero(numero)
                .folio("CP-2026-0001")
                .motivo(MotivoReporte.YA_PAGUE)
                .fechaCreacion(creacion)
                .usuarioId(camila)
                .empresaId(UUID.randomUUID())
                .empresa("Inmobiliaria Altos del Maipo")
                .concepto("Gasto común")
                .total(100000)
                .fechaVencimiento(LocalDate.of(2026, 9, 30))
                .nombreUsuario("Camila Rojas")
                .usuarioCorreo("crojas@correo.ejemplo")
                .build();
        r.registrarCambio(EstadoReporte.ABIERTO, camila, "crojas@correo.ejemplo", creacion);
        repository.save(r);
    }
}
