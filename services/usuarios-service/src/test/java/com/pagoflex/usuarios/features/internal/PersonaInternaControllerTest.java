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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PersonaInternaControllerTest {
    private static final String CLAVE = "test-only-internal-api-key";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PersonaRepository personaRepo;

    private Users camila;

    @BeforeEach
    void preparar() {
        userRepo.deleteAll();
        personaRepo.deleteAll();
        Persona persona = personaRepo.save(Persona.builder()
                .rut("15834207-3").nombres("Camila Andrea").apellidos("Rojas Peña").telefono("+56 9 0000 0101").build());
        camila = userRepo.save(Users.builder()
                .email("crojas@correo.ejemplo").password("x").role(Role.USUARIO_FINAL).persona(persona).build());
    }

    @Test
    void sinClaveInternaNoResponde() throws Exception {
        mvc.perform(get("/api/v1/internal/personas/rut/15834207-3"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/internal/personas/rut/15834207-3").header("X-Internal-Api-Key", "otra"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buscaPorRutConPuntosYTraeLaCuenta() throws Exception {
        mvc.perform(get("/api/v1/internal/personas/rut/15.834.207-3").header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rut").value("15834207-3"))
                .andExpect(jsonPath("$.correo").value("crojas@correo.ejemplo"))
                .andExpect(jsonPath("$.usuarioId").value(camila.getId().toString()));
    }

    @Test
    void buscaPorUsuarioYPorCorreo() throws Exception {
        mvc.perform(get("/api/v1/internal/personas/usuario/" + camila.getId()).header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Camila Andrea"));
        mvc.perform(get("/api/v1/internal/usuarios/correo/CROJAS@correo.ejemplo").header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(camila.getPersona().getId().toString()));
        mvc.perform(get("/api/v1/internal/personas/usuario/" + UUID.randomUUID()).header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isNotFound());
    }

    @Test
    void creaPersonaSinCuentaUnaSolaVez() throws Exception {
        String cuerpo = """
                {"rut":"20.111.222-2","nombres":"Ana","apellidos":"Pérez","telefono":"+56 9 1111 2222","correo":"ana@correo.ejemplo"}
                """;
        mvc.perform(post("/api/v1/internal/personas").header("X-Internal-Api-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rut").value("20111222-2"))
                .andExpect(jsonPath("$.correo").value("ana@correo.ejemplo"))
                .andExpect(jsonPath("$.usuarioId").value(nullValue()));
        mvc.perform(post("/api/v1/internal/personas").header("X-Internal-Api-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
    }

    @Test
    void rechazaRutInvalidoYDatosVacios() throws Exception {
        mvc.perform(get("/api/v1/internal/personas/rut/12345678-9").header("X-Internal-Api-Key", CLAVE))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/internal/personas").header("X-Internal-Api-Key", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rut\":\"20111222-0\"}"))
                .andExpect(status().isBadRequest());
    }
}
