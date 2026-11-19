package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.features.compromiso.dto.request.AnularCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.request.RegistrarCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.response.CompromisoResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.PersonaEncontradaResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.ResumenCobranzaResponse;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.empresa.dto.response.EmpresaResponse;
import com.pagoflex.security.AuthenticatedUser;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface CobranzaEjecutivoService {
    EmpresaResponse empresa(UUID ejecutivoId);

    PersonaEncontradaResponse buscarPersona(UUID ejecutivoId, String rut);

    ResumenCobranzaResponse resumen(UUID ejecutivoId, YearMonth mes);

    List<CompromisoResponse> listar(UUID ejecutivoId, YearMonth mes, EstadoCompromiso estado);

    CompromisoResponse detalle(UUID ejecutivoId, String folio);

    CompromisoResponse registrar(AuthenticatedUser ejecutivo, RegistrarCompromisoRequest request);

    CompromisoResponse anular(AuthenticatedUser ejecutivo, String folio, AnularCompromisoRequest request);
}
