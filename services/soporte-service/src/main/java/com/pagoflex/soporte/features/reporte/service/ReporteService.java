package com.pagoflex.soporte.features.reporte.service;

import com.pagoflex.security.AuthenticatedUser;
import com.pagoflex.soporte.features.reporte.dto.request.ReportarRequest;
import com.pagoflex.soporte.features.reporte.dto.request.ResolverRequest;
import com.pagoflex.soporte.features.reporte.dto.response.PaginaResponse;
import com.pagoflex.contracts.soporte.ParticipantesReporteInterno;
import com.pagoflex.soporte.features.reporte.dto.response.ReporteResponse;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;

import com.pagoflex.soporte.features.reporte.storage.ComprobanteStorage;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ReporteService {
    // ---------- usuario final ----------
    ReporteResponse reportar(AuthenticatedUser usuario, ReportarRequest request);

    PaginaResponse<ReporteResponse> misReportes(UUID usuarioId, int page, int size);

    ReporteResponse miReporte(UUID usuarioId, String numero);

    /// la usuaria adjunta la foto del comprobante a su reporte
    ReporteResponse subirComprobante(UUID usuarioId, String numero, MultipartFile archivo);

    // ---------- agente de soporte ----------
    PaginaResponse<ReporteResponse> bandeja(UUID agenteId, EstadoReporte estado, Boolean atrasado, int page, int size);

    ReporteResponse detalle(UUID agenteId, String numero);

    /// el agente revisa el comprobante para confirmar si el pago se hizo
    ComprobanteStorage.Archivo comprobante(UUID agenteId, String numero);

    ReporteResponse asignar(AuthenticatedUser agente, String numero);

    ReporteResponse atender(AuthenticatedUser agente, String numero);

    ReporteResponse resolver(AuthenticatedUser agente, String numero, ResolverRequest request);

    // ---------- ejecutivo de la empresa ----------
    PaginaResponse<ReporteResponse> deMiEmpresa(UUID ejecutivoId, EstadoReporte estado, int page, int size);

    ReporteResponse reporteActivo(UUID ejecutivoId, String folio);

    // ---------- interno ----------
    ParticipantesReporteInterno participantes(String numero);
}
