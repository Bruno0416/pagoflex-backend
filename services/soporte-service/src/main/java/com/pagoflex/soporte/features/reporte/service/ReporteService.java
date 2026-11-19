package com.pagoflex.soporte.features.reporte.service;

import com.pagoflex.security.AuthenticatedUser;
import com.pagoflex.soporte.features.reporte.dto.request.ReportarRequest;
import com.pagoflex.soporte.features.reporte.dto.request.ResolverRequest;
import com.pagoflex.soporte.features.reporte.dto.response.ReporteResponse;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;

import java.util.List;

public interface ReporteService {
    // ---------- usuario final ----------
    ReporteResponse reportar(AuthenticatedUser usuario, ReportarRequest request);

    List<ReporteResponse> misReportes(String correo);

    // ---------- agente de soporte ----------
    List<ReporteResponse> bandeja(EstadoReporte estado);

    ReporteResponse detalle(String numero);

    ReporteResponse tomar(String agente, String numero);

    ReporteResponse resolver(String agente, String numero, ResolverRequest request);
}
