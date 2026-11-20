package com.pagoflex.usuarios.features.internal;

import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.repository.PersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Rutas internas de personas con seguridad y base de datos reales (H2): las usan cobranza y soporte
@SpringBootTest
@AutoConfigureMockMvc
class InternalPersonIntegrationTest {
    private static final String CLAVE = "test-only-internal-api-key";
    private static final String BASE = "/api/v1/usuarios/internal/personas";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PersonaRepository personaRepo;
    @MockitoBean
    private StringRedisTemplate redis;

    private Users camila;
    private Users ejecutivo;
    private final UUID empresa = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        userRepo.deleteAll();
        personaRepo.deleteAll();
        Persona p = personaRepo.save(Persona.builder().rut("15834207-3").nombres("Camila Andrea")
                .apellidos("Rojas Peña").telefono("+56900000101").build());
        camila = userRepo.save(Users.builder().email("crojas@correo.ejemplo").password("x")
                .role(Role.USUARIO_FINAL).persona(p).build());
        Persona pe = personaRepo.save(Persona.builder().rut("14629853-2").nombres("Paula").apellidos("Saavedra")
                .empresaId(empresa).build());
        ejecutivo = userRepo.save(Users.builder().email("psaavedra@altosmaipo.ejemplo").password("x")
                .role(Role.EJECUTIVO).persona(pe).build());
    }

    // ---------- seguridad ----------
    @Test
    void sinClaveInternaOConUnJwtNoResponde() throws Exception {
        mvc.perform(get(BASE + "/por-rut/15834207-3")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/por-rut/15834207-3").header("X-Internal-Api-Key", "otra"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/por-rut/15834207-3").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- consultas ----------
    @Test
    void buscaPorRutConPuntosYTraeLaCuenta() throws Exception {
        mvc.perform(get(BASE + "/por-rut/15.834.207-3").header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rut").value("15834207-3"))
                .andExpect(jsonPath("$.idUsuario").value(camila.getId().toString()))
                .andExpect(jsonPath("$.correoCuenta").value("crojas@correo.ejemplo"))
                .andExpect(jsonPath("$.idEmpresa").value(nullValue()));
    }

    @Test
    void buscaPorUsuarioYPorCorreoDeLaCuenta() throws Exception {
        mvc.perform(get(BASE + "/por-usuario/" + ejecutivo.getId()).header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPersona").value(ejecutivo.getPersona().getId().toString()))
                .andExpect(jsonPath("$.idEmpresa").value(empresa.toString()));
        mvc.perform(get(BASE + "/por-correo/PSAAVEDRA@altosmaipo.ejemplo").header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(ejecutivo.getId().toString()));
        mvc.perform(get(BASE + "/por-usuario/" + UUID.randomUUID()).header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/por-correo/nadie@x.cl").header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscaPorIdDePersona() throws Exception {
        mvc.perform(get(BASE + "/" + camila.getPersona().getId()).header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombres").value("Camila Andrea"));
        mvc.perform(get(BASE + "/" + UUID.randomUUID()).header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isNotFound());
    }

    // ---------- alta RN-20 sin cuenta y sin duplicados ----------
    @Test
    void creaPersonasSinCuentaYNuncaDuplicaElRut() throws Exception {
        String cuerpo = """
                {"rut":"20111222-2","nombres":"Ana","apellidos":"Paz","telefono":"+56900001000"}
                """;
        mvc.perform(post(BASE).header("X-Internal-Api-Key", CLAVE).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(nullValue()));
        // el mismo RUT otra vez: conflicto y una sola fila
        mvc.perform(post(BASE).header("X-Internal-Api-Key", CLAVE).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
        assertThat(personaRepo.findAll().stream().filter(p -> p.getRut().equals("20111222-2"))).hasSize(1);
        // la persona sin cuenta se encuentra por rut, sin usuario ni empresa
        mvc.perform(get(BASE + "/por-rut/20111222-2").header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(nullValue()))
                .andExpect(jsonPath("$.idEmpresa").value(nullValue()));
    }

    @Test
    void rechazaRutInvalidoYDatosIncompletos() throws Exception {
        mvc.perform(post(BASE).header("X-Internal-Api-Key", CLAVE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"99999999-8\",\"nombres\":\"A\",\"apellidos\":\"B\",\"telefono\":\"+56900001000\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE).header("X-Internal-Api-Key", CLAVE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"20111222-2\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/por-rut/12").header("X-Internal-Api-Key", CLAVE)).andExpect(status().isBadRequest());
    }
}
