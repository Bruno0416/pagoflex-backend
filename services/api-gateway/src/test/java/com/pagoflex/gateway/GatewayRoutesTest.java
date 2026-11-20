package com.pagoflex.gateway;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/// Rutas reales del gateway (application.yaml principal): cada ruta pública llega al servicio correcto y en orden
class GatewayRoutesTest {
    private static final PathPatternParser PARSER = new PathPatternParser();
    private static final List<Ruta> RUTAS = new ArrayList<>();

    record Ruta(String id, String uri, List<String> patrones) {
    }

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void cargar() throws Exception {
        // 1. leer las rutas del application.yaml principal del gateway
        Map<String, Object> raiz = new Yaml().load(new FileInputStream("src/main/resources/application.yaml"));
        Map<String, Object> gateway = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>)
                raiz.get("spring")).get("cloud")).get("gateway");
        Map<String, Object> webmvc = (Map<String, Object>) ((Map<String, Object>) gateway.get("server")).get("webmvc");
        for (Map<String, Object> ruta : (List<Map<String, Object>>) webmvc.get("routes")) {
            List<String> patrones = new ArrayList<>();
            for (String p : (List<String>) ruta.get("predicates")) {
                if (p.startsWith("Path=")) patrones.addAll(List.of(p.substring(5).split(",")));
            }
            RUTAS.add(new Ruta((String) ruta.get("id"), (String) ruta.get("uri"), patrones));
        }
    }

    @Test
    void usuariosYReconocimientosConservanSusRutas() {
        assertThat(destino("/api/v1/auth/iniciar-sesion")).isEqualTo("usuarios-service");
        assertThat(destino("/api/v1/auth/registro")).isEqualTo("usuarios-service");
        assertThat(destino("/api/v1/usuarios/me")).isEqualTo("usuarios-service-private");
        assertThat(destino("/api/v1/reconocimientos/me")).isEqualTo("reconocimientos-service");
    }

    @Test
    void rutasDelUsuarioFinalYDelEjecutivoLlegaAcobranza() {
        for (String ruta : new String[]{"/api/v1/compromisos/me", "/api/v1/compromisos/me/CP-2026-0001/pagos",
                "/api/v1/pagos/me", "/api/v1/comprobantes/CPR-000001", "/api/v1/anulaciones/me",
                "/api/v1/cumplimiento/me", "/api/v1/empresas/me", "/api/v1/empresas/me/compromisos",
                "/api/v1/empresas/me/cobranza", "/api/v1/empresas/me/personas/12345678-5"}) {
            assertThat(destino(ruta)).as(ruta).isEqualTo("cobranza-service");
        }
    }

    @Test
    void soporteVaAntesQueCobranzaEnLosSegmentosCompartidosDeEmpresasMe() {
        for (String ruta : new String[]{"/api/v1/reportes", "/api/v1/reportes/me", "/api/v1/reportes/R-0001/asignar",
                "/api/v1/empresas/me/reportes", "/api/v1/empresas/me/reportes/R-0001",
                "/api/v1/empresas/me/compromisos/CP-2026-0001/reporte-activo"}) {
            assertThat(destino(ruta)).as(ruta).isEqualTo("soporte-service");
        }
        assertThat(indice("soporte-service")).isLessThan(indice("cobranza-service"));
    }

    @Test
    void ningunaRutaInternaTieneRutaPropiaEnElGateway() {
        for (String ruta : new String[]{"/api/v1/internal/empresas/codigos/validar",
                "/api/v1/internal/usuarios/x/hechos-pago", "/api/v1/internal/compromisos/CP-1/reportabilidad",
                "/api/v1/internal/reconocimientos/evaluar", "/api/v1/email/internal/send"}) {
            assertThat(destino(ruta)).as(ruta).isNull();
        }
    }

    // --- helpers ---
    private static String destino(String ruta) {
        PathContainer path = PathContainer.parsePath(ruta);
        return RUTAS.stream()
                .filter(r -> r.patrones().stream().anyMatch(p -> PARSER.parse(p.trim()).matches(path)))
                .map(Ruta::id).findFirst().orElse(null);
    }

    private static int indice(String id) {
        for (int i = 0; i < RUTAS.size(); i++) if (RUTAS.get(i).id().equals(id)) return i;
        return -1;
    }
}
