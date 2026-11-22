package com.pagoflex.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalApiKeyFilterTest {
    private final InternalApiKeyFilter filter = new InternalApiKeyFilter("clave-de-servicio");

    // ---------- clave interna ----------
    @Test
    void rejectsMissingKey() throws Exception {
        var response = run(null);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void rejectsWrongKey() throws Exception {
        var response = run("otra-clave");
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void acceptsExactKey() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/internal/x");
        request.addHeader(InternalApiKeyFilter.HEADER, "clave-de-servicio");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void blankConfiguredKeyFailsFast() {
        assertThatThrownBy(() -> new InternalApiKeyFilter(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InternalApiKeyFilter(null)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- helper ---
    private MockHttpServletResponse run(String key) throws Exception {
        // 1. armar la peticion con o sin clave
        var request = new MockHttpServletRequest("POST", "/api/v1/internal/x");
        if (key != null) request.addHeader(InternalApiKeyFilter.HEADER, key);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        // 2. ejecutar el filtro y comprobar que la cadena no continua
        filter.doFilter(request, response, chain);
        assertThat(chain.getRequest()).isNull();
        return response;
    }
}
