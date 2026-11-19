package com.pagoflex.usuarios.features.internal.controller;

import com.pagoflex.usuarios.features.internal.service.InternalPersonService;
import com.pagoflex.usuarios.features.internal.dto.response.PersonResponse;
import com.pagoflex.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternalPersonController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InternalPersonControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean InternalPersonService service;

    // ---------- contratos internos ----------
    @Test
    void createsPersonAndReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.create(any())).thenReturn(new PersonResponse(id, "99999999-9", "Usuario", "Prueba", null, "+56912345678"));
        mvc.perform(post("/api/v1/usuarios/internal/personas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"99999999-9\",\"nombres\":\"Usuario\",\"apellidos\":\"Prueba\",\"telefono\":\"+56912345678\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPersona").value(id.toString()));
    }

    @Test
    void rejectsMissingPhone() throws Exception {
        mvc.perform(post("/api/v1/usuarios/internal/personas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"99999999-9\",\"nombres\":\"Usuario\",\"apellidos\":\"Prueba\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.telefono").exists());
        verifyNoInteractions(service);
    }

    @Test
    void readsPersonById() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.byId(id)).thenReturn(new PersonResponse(id, "99999999-9", "Usuario", "Prueba", null, "+56912345678"));
        mvc.perform(get("/api/v1/usuarios/internal/personas/" + id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idPersona").value(id.toString()));
    }
}
