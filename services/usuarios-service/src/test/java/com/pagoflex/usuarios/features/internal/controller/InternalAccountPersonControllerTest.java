package com.pagoflex.usuarios.features.internal.controller;

import com.pagoflex.usuarios.features.internal.dto.response.AccountPersonResponse;
import com.pagoflex.usuarios.features.internal.exceptions.PersonNotFound;
import com.pagoflex.usuarios.features.internal.service.InternalPersonService;
import com.pagoflex.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// persona de una cuenta para cobranza y soporte
@WebMvcTest(InternalPersonController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InternalAccountPersonControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean InternalPersonService service;

    @Test
    void readsPersonOfAccountWithCompany() throws Exception {
        UUID user = UUID.randomUUID();
        UUID company = UUID.randomUUID();
        when(service.byUser(user)).thenReturn(new AccountPersonResponse(user, UUID.randomUUID(), "14629853-2",
                "Paula Andrea", "Saavedra Núñez", "psaavedra@altosmaipo.ejemplo", "+56900000401", company, "EJECUTIVO"));
        mvc.perform(get("/api/v1/usuarios/internal/personas/por-usuario/" + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(user.toString()))
                .andExpect(jsonPath("$.idEmpresa").value(company.toString()))
                .andExpect(jsonPath("$.correo").value("psaavedra@altosmaipo.ejemplo"));
    }

    @Test
    void unknownEmailIs404() throws Exception {
        when(service.byEmail("nadie@correo.ejemplo")).thenThrow(new PersonNotFound());
        mvc.perform(get("/api/v1/usuarios/internal/personas/por-correo/nadie@correo.ejemplo"))
                .andExpect(status().isNotFound());
    }
}
